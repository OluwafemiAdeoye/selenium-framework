package com.framework.selenium.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 */
public final class JsonDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonDataReader() {
    }

    // ---- Keyed-object format readers ----

    /**
     * Reads a keyed-object JSON file into a Map of Maps.
     * The outer key is the test case name (e.g. "validUser"), and the inner Map
     * contains the test data fields (e.g. username, password, expectedOutcome, description).
     */
    public static Map<String, Map<String, String>> readKeyedJson(String filePath) throws IOException {
        return MAPPER.readValue(new File(filePath),
                new TypeReference<LinkedHashMap<String, Map<String, String>>>() {
                });
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
        return MAPPER.readValue(new File(filePath),
                new TypeReference<List<Map<String, String>>>() {
                });
    }

    public static <T> List<T> readJsonArray(String filePath, Class<T> clazz) throws IOException {
        return MAPPER.readValue(new File(filePath),
                MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
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
