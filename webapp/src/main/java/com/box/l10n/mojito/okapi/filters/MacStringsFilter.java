package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.regex.RegexFilter;

/**
 * Overrides {@link RegexFilter} to handle escape/unescape special characters
 *
 * @author jyi
 */
public class MacStringsFilter extends RegexEscapeDoubleQuoteFilter {

    public static final String FILTER_CONFIG_ID = "okf_regex@mojito";

    private static final String LOCATION_PATTERN = "/\\* Location: (?<location>.*?) \\*/";
    private static final String LOCATION_GROUP_NAME = "location";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<>();
        list.add(new FilterConfiguration(getName() + "-macStrings",
                getMimeType(),
                getClass().getName(),
                "Text (Mac Strings)",
                "Configuration for Macintosh .strings files.",
                "macStrings_mojito.fprm"));
        return list;
    }

    @Override
    public Event next() {
        Event event = super.next();

        if (event.getEventType() == EventType.TEXT_UNIT) {
            TextUnit textUnit = (TextUnit) event.getTextUnit();
            addUsagesToTextUnit(textUnit);
        }

        return event;
    }

    private void addUsagesToTextUnit(TextUnit textUnit) {
        if (!textUnit.getName().contains("/* Location:")) {
            return;
        }

        String name = textUnit.getName().split("\"")[1];
        String usageString = textUnit.getName().split("\"")[0];
        Set<String> usages = new LinkedHashSet<>();

        Pattern pattern = Pattern.compile(LOCATION_PATTERN);
        Matcher matcher = pattern.matcher(usageString);

        while (matcher.find()) {
            String usage = matcher.group(LOCATION_GROUP_NAME);
            usages.add(usage);
        }

        textUnit.setName(name);
        textUnit.setAnnotation(new UsagesAnnotation(usages));

    }

}
