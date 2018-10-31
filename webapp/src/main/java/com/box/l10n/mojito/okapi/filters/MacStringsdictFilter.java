package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import net.sf.okapi.common.*;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.*;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jyi
 */
public class MacStringsdictFilter extends XMLFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(MacStringsdictFilter.class);

    public static final String FILTER_CONFIG_ID = "okf_xml@mojito";
    public static final String MAC_STRINGSDICT_CONFIG_FILE_NAME = "macStringsdict_mojito.fprm";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    LocaleId targetLocale;
    List<Event> eventQueue = new ArrayList<>();

    @Autowired
    TextUnitUtils textUnitUtils;

    boolean hasAnnotation;

    /**
     * Overriding to include only mac stringsdict, resx, xtb and AndroidStrings filters
     *
     * @return
     */
    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<>();
        list.add(new FilterConfiguration(getName() + "-stringsdict",
                getMimeType(),
                getClass().getName(),
                "Apple Stringsdict",
                "Configuration for Apple Stringsdict files.",
                MAC_STRINGSDICT_CONFIG_FILE_NAME,
                ".stringsdict;"));
        return list;
    }

    @Override
    public boolean hasNext() {
        return !eventQueue.isEmpty() || super.hasNext();
    }

    @Override
    public Event next() {
        Event event;

        if (eventQueue.isEmpty()) {
            readNextEvents();
        }

        event = eventQueue.remove(0);

        return event;
    }

    @Override
    public void open(RawDocument input) {
        super.open(input);
        targetLocale = input.getTargetLocale();
        logger.debug("target locale: ", targetLocale);
        hasAnnotation = input.getAnnotation(CopyFormsOnImport.class) != null;
    }

    private void readNextEvents() {
        Event next = getNextWithProcess();

        if (next.isTextUnit() && isPluralGroupStarting(next.getResource())) {
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    private String unescape(String text) {
        // unescape double or single quotes
        String unescapedText = text.replaceAll("(\\\\)(\"|')", "$2");
        // unescape \n
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
        // unescape \r
        unescapedText = unescapedText.replaceAll("\\\\r", "\r");
        return unescapedText;
    }

    /**
     * Extract the note from XML comments only if there is no note on the text
     * unit. In other words if a note was specify via attribute like description
     * for android it won't be overridden by an comments present in the XML
     * file.
     *
     * @param textUnit the text unit from which comments should be extracted
     */
    protected void extractNoteFromXMLCommentInSkeletonIfNone(TextUnit textUnit) {

        String skeleton = textUnit.getSkeleton().toString();

        if (textUnit.getProperty(Property.NOTE) == null) {
            String note = getNoteFromXMLCommentsInSkeleton(skeleton);
            if (note != null) {
                textUnit.setProperty(new Property(Property.NOTE, note));
            }
        }
    }

    // Match single or multi-line comments
    private static final String XML_COMMENT_PATTERN = "<!--(?<comment>(.*?\\s)*?)-->";
    private static final String XML_COMMENT_GROUP_NAME = "comment";
    // Match single or multiple location (additional locations on next line)
    static final String USAGE_LOCATION_PATTERN = "Location: (?<location>(.*?\\s)*?)-->";
    static final String USAGE_LOCATION_GROUP_NAME = "location";

    /**
     * Gets the note from the XML comments in the skeleton.
     *
     * @param skeleton that may contains comments
     * @return the note or <code>null</code>
     */
    protected String getNoteFromXMLCommentsInSkeleton(String skeleton) {

        String note = null;

        StringBuilder commentBuilder = new StringBuilder();

        Pattern pattern = Pattern.compile(XML_COMMENT_PATTERN);
        Matcher matcher = pattern.matcher(skeleton);

        while (matcher.find()) {
            if (commentBuilder.length() > 0) {
                commentBuilder.append(" ");
            }
            commentBuilder.append(matcher.group(XML_COMMENT_GROUP_NAME).trim());
        }

        if (commentBuilder.length() > 0) {
            note = commentBuilder.toString();
        }

        return note;
    }

    private void processTextUnit(Event event) {
        if (event != null && event.isTextUnit()) {
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            String sourceString = textUnit.getSource().toString();
            // if source has escaped double-quotes, single-quotes, \r or \n, unescape
            TextContainer source = new TextContainer(unescape(sourceString));
            textUnit.setSource(source);
            extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
            addUsagesToTextUnit(textUnit);
        }
    }

    private void setUsagesAnnotationOnTextUnit(Set<String> usagesFromSkeleton, ITextUnit textUnit) {
        textUnit.setAnnotation(new UsagesAnnotation((usagesFromSkeleton)));
    }

    void addUsagesToTextUnit(TextUnit textUnit) {
        Set<String> usageLocationsFromSkeleton = getUsagesFromSkeleton(textUnit.getSkeleton().toString());
        setUsagesAnnotationOnTextUnit(usageLocationsFromSkeleton, textUnit);
    }

    Set<String> getUsagesFromSkeleton(String skeleton) {
        Set<String> usages = new LinkedHashSet<>();

        Pattern pattern = Pattern.compile(USAGE_LOCATION_PATTERN);
        Matcher matcher = pattern.matcher(skeleton);

        if (matcher.find()) {
            String[] locations = matcher.group(USAGE_LOCATION_GROUP_NAME).split("\n");
            for (int i = 0; i < locations.length; i++) {
                // There should be no whitespace characters in usages, so remove them
                usages.add(locations[i].trim());
            }
        }
        return usages;
    }

    private Event getNextWithProcess() {
        Event next = super.next();
        processTextUnit(next);
        return next;
    }

    private void readPlurals(Event next) {

        List<Event> pluralEvents = new ArrayList<>();

        // read others until the end
        do {
            pluralEvents.add(next);
            next = getNextWithProcess();
        } while (next != null && !isPluralGroupEnding(next.getResource()));

        // that doesn't contain last
        pluralEvents = adaptPlurals(pluralEvents);

        eventQueue.addAll(pluralEvents);

        if (isPluralGroupStarting(next.getResource())) {
            readPlurals(next);
        } else {
            eventQueue.add(next);
        }
    }

    // finds start of plural group
    protected boolean isPluralGroupStarting(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Pattern p = Pattern.compile("<key>NSStringFormatSpecTypeKey</key>\n" +
                "<string>NSStringPluralRuleType</string>\n" +
                "<key>NSStringFormatValueTypeKey</key>\n" +
                "<string>d</string>");
        Matcher matcher = p.matcher(toString);
        boolean startPlural = matcher.find();
        return startPlural;
    }


    //finds end of plural group
    protected boolean isPluralGroupEnding(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Pattern p = Pattern.compile("</dict>");
        Matcher matcher = p.matcher(toString);
        boolean endPlural = matcher.find();
        return endPlural;
    }

    protected List<Event> adaptPlurals(List<Event> pluralEvents) {
        logger.debug("Adapt plural forms if needed");
        PluralsHolder pluralsHolder = new MacStringsdictPluralsHolder();
        pluralsHolder.loadEvents(pluralEvents); // make sure get proper number
        logger.debug("target locale: ", targetLocale);
        List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
        return completedForms;
    }

    class MacStringsdictPluralsHolder extends PluralsHolder {
        String firstForm = null;
        String comments = null;

        @Override
        protected void loadEvents(List<Event> pluralEvents) {

            if (!pluralEvents.isEmpty()) {
                Event firstEvent = pluralEvents.get(0);
                firstForm = getPluralFormFromSkeleton(firstEvent.getResource());
                ITextUnit firstTextUnit = firstEvent.getTextUnit();
                comments = textUnitUtils.getNote(firstTextUnit);
            }

            super.loadEvents(pluralEvents);
        }

        String getPluralFormFromSkeleton(IResource resource) {
            String toString = resource.getSkeleton().toString();
            Pattern p = Pattern.compile("<key>");
            Matcher matcher = p.matcher(toString);
            String res = null;
            if (matcher.find()) {
                res = matcher.group(1);
            }
            return res;
        }

        @Override
        protected Event createCopyOf(Event event, String sourceForm, String targetForm) {
            logger.debug("Create copy of: {}, source form: {}, target form: {}", event.getTextUnit().getName(), sourceForm, targetForm);
            ITextUnit textUnit = event.getTextUnit().clone();
            renameTextUnit(textUnit, sourceForm, targetForm);
            updateItemFormInSkeleton(textUnit);
            replaceFormInSkeleton((GenericSkeleton) textUnit.getSkeleton(), sourceForm, targetForm);
            Event copyOfOther = new Event(EventType.TEXT_UNIT, textUnit);
            return copyOfOther;
        }

        void updateItemFormInSkeleton(ITextUnit textUnit) {
            boolean ignore = true;
            GenericSkeleton genericSkeleton = (GenericSkeleton) textUnit.getSkeleton();
            for (GenericSkeletonPart genericSkeletonPart : genericSkeleton.getParts()) {
                String partString = genericSkeletonPart.toString();
                Pattern p = Pattern.compile("<key>quantity.+?</key>");
                Matcher matcher = p.matcher(partString);
                if (matcher.find()) {
                    String match = matcher.group(1);
                    genericSkeletonPart.setData(match);
                    ignore = false;
                }
                if (ignore) {
                    genericSkeletonPart.setData("");
                }
            }
        }

        @Override
        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
            for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                StringBuilder sb = part.getData();
                //TODO make more flexible
                // todo check this works with the parser
                String str = sb.toString().replace("<key>" + sourceForm + "</key>", "<key>" + targetForm + "</key>");
                sb.replace(0, sb.length(), str);
            }
        }

    }
}
