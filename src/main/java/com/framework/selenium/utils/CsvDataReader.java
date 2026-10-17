package com.framework.selenium.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads CSV test data files.
 * Supports both raw String[] and header-mapped Map&lt;String, String&gt; formats.
 *
 * <h3>Recommended: header-mapped reading</h3>
 * <pre>
 * username,password,expectedOutcome,description
 * tomsmith,SuperSecretPassword!,success,Valid user with correct credentials
 * invalid_user,wrong_password,failure,Non-existent user
 * </pre>
 * Use {@link #readCsvAsMaps(String)} to get each row as a Map keyed by column header.
 */
public final class CsvDataReader {

    private CsvDataReader() {
    }

    // ---- Header-mapped readers (recommended) ----

    /**
     * Reads CSV rows as a list of Maps, where each Map is keyed by the column header.
     * For example, a row with headers "username,password,expectedOutcome" becomes:
     * {@code {"username": "tomsmith", "password": "pass123", "expectedOutcome": "success"}}
     */
    public static List<Map<String, String>> readCsvAsMaps(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                return new ArrayList<>();
            }

            String[] headers = allRows.getFirst();
            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    rowMap.put(headers[j].trim(), j < row.length ? row[j] : "");
                }
                result.add(rowMap);
            }
            return result;
        }
    }

    /**
     * Returns header-mapped CSV data as Object[][] suitable for TestNG DataProvider.
     * Each element is a Map&lt;String, String&gt; keyed by column header — identical
     * format to what JsonDataReader returns, so tests can consume both interchangeably.
     */
    public static Object[][] readCsvAsMapsDataProvider(String filePath) throws IOException, CsvException {
        List<Map<String, String>> rows = readCsvAsMaps(filePath);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    // ---- Raw readers (legacy) ----

    /**
     * Returns all rows (excluding header) as raw String arrays.
     * Prefer {@link #readCsvAsMaps(String)} for named-column access.
     */
    public static List<String[]> readCsvData(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> allRows = reader.readAll();
            if (!allRows.isEmpty()) {
                allRows.removeFirst();
            }
            return allRows;
        }
    }

    /**
     * Returns raw data as Object[][] suitable for TestNG DataProvider.
     * Prefer {@link #readCsvAsMapsDataProvider(String)} for named-column access.
     */
    public static Object[][] readCsvAsDataProvider(String filePath) throws IOException, CsvException {
        List<String[]> rows = readCsvData(filePath);
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }
        return data;
    }

    /**
     * Returns the header row from a CSV file.
     */
    public static String[] readCsvHeaders(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            return reader.readNext();
        }
    }
}
