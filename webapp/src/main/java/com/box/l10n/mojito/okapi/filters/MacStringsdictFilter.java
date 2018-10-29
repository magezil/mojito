package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.*;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.*;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        String unescapedText = text.replaceAll("(\\\\)(\"|')", "$2");
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
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

    private static final String XML_COMMENT_PATTERN = "<!--(?<comment>(.*?\\s)*?)-->";
    private static final String XML_COMMENT_GROUP_NAME = "comment";
    static final String USAGE_LOCATION_GROUP_NAME = "location";
    static final String USAGE_LOCATION_PATTERN = "Location: (?<location>(.*?\\s)*?)-->";

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
//            String unescapedSourceString = unescape(sourceString);
            TextContainer source = new TextContainer(unescape(sourceString));
            textUnit.setSource(source);
            extractNoteFromXMLCommentInSkeletonIfNone(textUnit);
        }
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

    protected boolean isPluralGroupStarting(IResource resource) {
        String toString = resource.getSkeleton().toString();
        Pattern p = Pattern.compile("<dict>");
        Matcher matcher = p.matcher(toString);
        boolean startPlural = matcher.find();
        return startPlural;
    }

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
        pluralsHolder.loadEvents(pluralEvents);
        logger.debug("target locale: ", targetLocale);
        List<Event> completedForms = pluralsHolder.getCompletedForms(targetLocale);
        return completedForms;
    }

    class MacStringsdictPluralsHolder extends PluralsHolder {
        @Override
        void adaptTextUnitToCLDRForm(ITextUnit textUnit, String cldrPluralForm) {
//            nothing in android
//            from PO
//            if (!"one".equals(cldrPluralForm)) {
//                // source should always be plural form unless for "one" form,
//                // this is needed for language with only one entry like
//                // japanese: [0] --> other
//                logger.debug("Set message plural: {}", msgIDPlural);
//                textUnit.setSource(new TextContainer(msgIDPlural));
//            }
        }

        void replaceFormInSkeleton(GenericSkeleton genericSkeleton, String sourceForm, String targetForm) {
//            from Android
            for (GenericSkeletonPart part : genericSkeleton.getParts()) {
                StringBuilder sb = part.getData();
                //TODO make more flexible
                String str = sb.toString().replace(sourceForm + "\"", targetForm + "\"");
                sb.replace(0, sb.length(), str);
            }

//            from PO
//            logger.debug("Replace in skeleton form: {} to {} ({})", sourceForm, targetForm, poPluralRule.cldrFormToPoForm(targetForm));
//
//            String cldrFormToGettextForm = poPluralRule.cldrFormToPoForm(targetForm);
//
//            if (cldrFormToGettextForm != null) {
//                for (GenericSkeletonPart part : genericSkeleton.getParts()) {
//                    StringBuilder sb = part.getData();
//                    String str = sb.toString().replaceAll("msgstr\\[\\d\\]", "msgstr[" + cldrFormToGettextForm + "]");
//                    sb.replace(0, sb.length(), str);
//                }
//            } else {
//                logger.debug("No replacement, no PO idx for CLDR form: {}", targetForm);
//            }
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
                usages.add(locations[i].replaceAll("\\s", ""));
            }
        }
        return usages;
    }
}
