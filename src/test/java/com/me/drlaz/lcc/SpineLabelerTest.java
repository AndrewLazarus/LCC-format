/*
 * Copyright (c) 2025. Andrew J. Lazarus. All rights reserved.
 */

package com.me.drlaz.lcc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the regexes used by SpineLabeler
 */
class SpineLabelerTest {

    private static final List<String> MASTER_REGEX_FIELDS = List.of("cl", "topic", "ctr", "yr", "xtra");
    private static final List<String> CTR_REGEX_FIELDS = List.of("dot", "subtop", "ctr");
    static private Pattern master = null;
    private static Pattern doubleCutter = null;

    {
        Pattern master1;
        Pattern doubleCutter1;
        try {
            Field f1 = SpineLabeler.class.getDeclaredField("master");
            f1.setAccessible(true);
            master1 = (Pattern) f1.get(null);
            Field f2 = SpineLabeler.class.getDeclaredField("doubleCutter");
            f2.setAccessible(true);
            doubleCutter1 = (Pattern) f2.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        master = master1;
        doubleCutter = doubleCutter1;
    }

    private SortedMap<String, String> namedFields(List<String> fields, MatchResult matchResult) {
        SortedMap<String, String> result = new TreeMap<>();
        for (String key : fields) {
            result.put(key, Optional.ofNullable(matchResult.group(key)).orElse(""));
        }
        return result;
    }

    private SortedMap<String, String> orderedFields(List<String> fields, Iterable<String> values) {
        Iterator<String> iterator = values.iterator();
        SortedMap<String, String> result = new TreeMap<>();
        for (String key : fields) {
            result.put(key, iterator.next());
        }
        return result;
    }

    private final Matcher masterMatcher = master.matcher("");
    private final Matcher doubleCutterMatcher = doubleCutter.matcher("");

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
                           QA241.9 B15, QA|241.9|B15||
                           QA241, QA|241|||
                           QA241.9 B15 2025 vol. 20, QA|241.9|B15|2025|vol. 20
                           """)
    void masterTest(String input, String strExpected) {
        masterMatcher.reset(input);
        assertTrue(masterMatcher.matches());
        assertEquals(MASTER_REGEX_FIELDS.size(), masterMatcher.groupCount());
        String[] elements = strExpected.split("[|]", -1);
        assertEquals(MASTER_REGEX_FIELDS.size(), elements.length);
        assertEquals(orderedFields(MASTER_REGEX_FIELDS, List.of(elements)),
                namedFields(MASTER_REGEX_FIELDS, masterMatcher));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
                           .A23B34, .|A23|B34
                           .A23 B34, .|A23|B34
                           .A23   B34, .|A23|B34
                           """)
    void doubleCutterTest(String input, String strExpected) {
        doubleCutterMatcher.reset(input);
        assertTrue(doubleCutterMatcher.matches());
        assertEquals(CTR_REGEX_FIELDS.size(), doubleCutterMatcher.groupCount());
        String[] elements = strExpected.split("[|]", -1);
        assertEquals(CTR_REGEX_FIELDS.size(), elements.length);
        assertEquals(orderedFields(CTR_REGEX_FIELDS, List.of(elements)),
                namedFields(CTR_REGEX_FIELDS, doubleCutterMatcher));
    }
}