package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.TextUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author jeanaurambault
 */
public class MacStringsdictFilterTest {



    @Test
    public void testGetCldrPluralFormOfEvent() {
        MacStringsdictFilter instance = new MacStringsdictFilter();
        MacStringsdictFilter.MacStringsdictPluralsHolder macStringsdictPluralsHolder = instance.new MacStringsdictPluralsHolder();
        TextUnit textUnit = new TextUnit();
        textUnit.setName("plural_asdf_somestring_one");
        Event event = new Event(EventType.TEXT_UNIT, textUnit );
        assertEquals("one", macStringsdictPluralsHolder.getCldrPluralFormOfEvent(event));
    }

    // TODO: uncomment and update once usages are added to text unit
//    @Test
//    public void getUsagesFromSkeleton() {
//        String skeleton = "<!-- Comments\n"
//                + "Location: path/to/file.java:49\n"
//                + "          path/to/file.java:72\n"
//                + "-->\n"
//                + "<key>plural_recipe_cook_hours</key>\n"
//                + "<dict>\n"
//                + "    <key>NSStringLocalizedFormatKey</key>\n"
//                + "    <string>%#@hours@ to cook</string>\n"
//                + "    <key>hours</key>\n"
//                + "    <dict>\n"
//                + "        <key>NSStringFormatSpecTypeKey</key>\n"
//                + "        <string>NSStringPluralRuleType</string>\n"
//                + "        <key>NSStringFormatValueTypeKey</key>\n"
//                + "        <string>d</string>\n"
//                + "        <key>one</key>\n"
//                + "        <string>%d hour to cook</string>\n"
//                + "        <key>other</key>\n"
//                + "        <string>%d hours to cook</string>\n"
//                + "    </dict>\n"
//                + "</dict>\n";
//
//        MacStringsdictFilter instance = new MacStringsdictFilter();
//        List<String> usages = new ArrayList<>(instance.getUsagesFromSkeleton(skeleton));
//
//        assertEquals(2, usages.size());
//        assertEquals("path/to/file.java:49", usages.get(0));
//        assertEquals("path/to/file.java:72", usages.get(1));
//    }
//
//    @Test
//    public void getUsagesFromSkeletonNone() {
//        String skeleton = "<!-- Comments\n"
//                + "-->\n"
//                + "<key>plural_recipe_cook_hours</key>\n"
//                + "<dict>\n"
//                + "    <key>NSStringLocalizedFormatKey</key>\n"
//                + "    <string>%#@hours@ to cook</string>\n"
//                + "    <key>hours</key>\n"
//                + "    <dict>\n"
//                + "        <key>NSStringFormatSpecTypeKey</key>\n"
//                + "        <string>NSStringPluralRuleType</string>\n"
//                + "        <key>NSStringFormatValueTypeKey</key>\n"
//                + "        <string>d</string>\n"
//                + "        <key>one</key>\n"
//                + "        <string>%d hour to cook</string>\n"
//                + "        <key>other</key>\n"
//                + "        <string>%d hours to cook</string>\n"
//                + "    </dict>\n"
//                + "</dict>\n";
//
//        MacStringsdictFilter instance = new MacStringsdictFilter();
//        List<String> usages = new ArrayList<>(instance.getUsagesFromSkeleton(skeleton));
//        assertEquals(0, usages.size());
//    }
}
