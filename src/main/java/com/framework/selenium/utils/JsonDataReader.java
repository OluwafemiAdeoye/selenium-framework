package com.framework.selenium.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads JSON test data files into Java structures.
 * Supports both array-based and keyed-object formats.
 *
 * <h3>Keyed-object format (recommended):</h3>
 * <pre>
 * {
 *   "validUser": { "username": "tom", "password": "pass", "expectedOutcome": "success", "description": "..." },
 *   "invalidUser": { "username": "bad", "password": "wrong", "expectedOutcome": "failure", "description": "..." }
 * }
 * </pre>
 *
 * <h3>Array format (legacy):</h3>
 * <pre>
 * [
 *   { "username": "tom", "password": "pass", "expectedOutcome": "success" },
 *   { "username": "bad", "password": "wrong", "expectedOutcome": "failure" }
 * ]
 * </pre>
 *
 * <h3>Placeholders:</h3>
 * Values may contain {@code ${NAME}} placeholders, which are resolved against
 * system properties then environment variables. This keeps real credentials out
 * of version control. See {@link #resolvePlaceholders(String)}.
 */
public final class JsonDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)}");

    private JsonDataReader() {
    }

    // ---- Keyed-object format readers ----

    /**
     * Reads a keyed-object JSON file into a Map of Maps.
     * The outer key is the test case name (e.g. "validUser"), and the inner Map
     * contains the test data fields (e.g. username, password, expectedOutcome, description).
     */
    public static Map<String, Map<String, String>> readKeyedJson(String filePath) throws IOException {
        Map<String, Map<String, String>> raw = MAPPER.readValue(new File(filePath),
                new TypeReference<LinkedHashMap<String, Map<String, String>>>() {
                });
        Map<String, Map<String, String>> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : raw.entrySet()) {
            resolved.put(entry.getKey(), resolvePlaceholders(entry.getValue()));
        }
        return resolved;
    }

    /**
     * Reads a specific entry by key from a keyed-object JSON file.
     * Returns null if the key is not found.
     */
    public static Map<String, String> readEntry(String filePath, String key) throws IOException {
        Map<String, Map<String, String>> allData = readKeyedJson(filePath);
        return allData.get(key);
    }

    /**
     * Converts a keyed-object JSON file into a flat list of Maps (for DataProvider compatibility).
     * Each Map entry includes a synthetic "_testCaseName" key with the original key name.
     */
    public static List<Map<String, String>> readKeyedJsonAsList(String filePath) throws IOException {
        Map<String, Map<String, String>> allData = readKeyedJson(filePath);
        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : allData.entrySet()) {
            Map<String, String> testCase = new LinkedHashMap<>(entry.getValue());
            testCase.put("_testCaseName", entry.getKey());
            result.add(testCase);
        }
        return result;
    }

    // ---- Array format readers (legacy) ----

    /**
     * Reads an array-based JSON file into a list of Maps.
     */
    public static List<Map<String, String>> readJsonArray(String filePath) throws IOException {
        List<Map<String, String>> raw = MAPPER.readValue(new File(filePath),
                new TypeReference<List<Map<String, String>>>() {
                });
        List<Map<String, String>> resolved = new ArrayList<>();
        for (Map<String, String> entry : raw) {
            resolved.add(resolvePlaceholders(entry));
        }
        return resolved;
    }

    public static <T> List<T> readJsonArray(String filePath, Class<T> clazz) throws IOException {
        return MAPPER.readValue(new File(filePath),
                MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    // ---- Placeholder resolution ----

    /**
     * Resolves {@code ${NAME}} placeholders in a test data value against, in order:
     * <ol>
     *   <li>System properties ({@code -DNAME=value})</li>
     *   <li>Environment variables</li>
     * </ol>
     * Unresolved placeholders are returned unchanged so the caller can detect them,
     * mirroring the credential resolution strategy used in {@code DriverFactory}.
     */
    public static String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String resolved = lookup(matcher.group(1));
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(resolved != null ? resolved : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Applies {@link #resolvePlaceholders(String)} to every value in a test data entry.
     */
    public static Map<String, String> resolvePlaceholders(Map<String, String> entry) {
        if (entry == null) {
            return null;
        }
        Map<String, String> resolved = new LinkedHashMap<>(entry);
        resolved.replaceAll((key, value) -> resolvePlaceholders(value));
        return resolved;
    }

    private static String lookup(String name) {
        String value = System.getProperty(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(name);
        return value != null && !value.isBlank() ? value : null;
    }

    // ---- Generic readers ----

    public static <T> T readJson(String filePath, Class<T> clazz) throws IOException {
        return MAPPER.readValue(new File(filePath), clazz);
    }

    public static Map<String, Object> readJsonAsMap(String filePath) throws IOException {
        return MAPPER.readValue(new File(filePath),
                new TypeReference<Map<String, Object>>() {
                });
    }
}
