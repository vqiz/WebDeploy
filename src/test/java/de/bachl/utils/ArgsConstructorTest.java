/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ArgsConstructorTest {

    @Test
    void parseEmptyArgs_returnsEmptyMap() {
        HashMap<String, String> result = new ArgsConstructor(new String[]{}).parse();
        assertTrue(result.isEmpty());
    }

    @Test
    void parseSingleFlag_withNoValue_mapsToBooleanTrue() {
        HashMap<String, String> result = new ArgsConstructor(new String[]{"--flag"}).parse();
        assertEquals("true", result.get("--flag"));
    }

    @Test
    void parseFlagWithSpaceSeparatedValue_mapsCorrectly() {
        HashMap<String, String> result = new ArgsConstructor(new String[]{"--flag", "value"}).parse();
        assertEquals("value", result.get("--flag"));
    }

    @Test
    void parseFlagWithEqualsValue_mapsCorrectly() {
        HashMap<String, String> result = new ArgsConstructor(new String[]{"--flag=value"}).parse();
        assertEquals("value", result.get("--flag"));
    }

    @Test
    void parseMultipleFlags_allPresentInMap() {
        HashMap<String, String> result = new ArgsConstructor(
                new String[]{"--host", "1.2.3.4", "--password", "secret"}).parse();
        assertEquals("1.2.3.4", result.get("--host"));
        assertEquals("secret", result.get("--password"));
    }

    @Test
    void parseMixedFlagsWithAndWithoutValues_parsedCorrectly() {
        HashMap<String, String> result = new ArgsConstructor(
                new String[]{"--flag1", "v1", "--flag2", "--flag3", "v3"}).parse();
        assertEquals("v1", result.get("--flag1"));
        assertEquals("true", result.get("--flag2"));
        assertEquals("v3", result.get("--flag3"));
    }

    @Test
    void parseSetupWithEqualsValues_allKeysPresent() {
        HashMap<String, String> result = new ArgsConstructor(
                new String[]{"--setup", "--host=1.2.3.4", "--password=root123"}).parse();
        assertEquals("true", result.get("--setup"));
        assertEquals("1.2.3.4", result.get("--host"));
        assertEquals("root123", result.get("--password"));
    }

    @Test
    void parseEqualsValueWithColonInValue_retainsFullValue() {
        HashMap<String, String> result = new ArgsConstructor(
                new String[]{"--target=http://localhost:8080"}).parse();
        assertEquals("http://localhost:8080", result.get("--target"));
    }

    @Test
    void parseNonFlagArguments_areIgnored() {
        HashMap<String, String> result = new ArgsConstructor(new String[]{"notaflag"}).parse();
        assertTrue(result.isEmpty());
    }
}
