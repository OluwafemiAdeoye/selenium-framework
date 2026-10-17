package com.framework.selenium.dataproviders;

import com.framework.selenium.constants.FrameworkConstants;
import com.framework.selenium.utils.JsonDataReader;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides test data from JSON files for data-driven tests.
 * Reads the keyed-object JSON format where each entry has a descriptive key.
 *
 * <p>Credentials are never committed. Values in {@code login-data.json} may use
 * {@code ${NAME}} placeholders, resolved from system properties or environment
 * variables by {@link JsonDataReader}. For local runs you can instead drop a
 * gitignored {@code login-data.local.json} alongside it — fields found there
 * override the same fields in the committed file.
 */
public class JsonDataProvider {

    private static final String LOGIN_DATA_PATH =
            FrameworkConstants.TEST_DATA_DIR + "/login-data.json";

    /** Optional, gitignored overlay holding real credentials for local runs. */
    private static final String LOGIN_DATA_LOCAL_PATH =
            FrameworkConstants.TEST_DATA_DIR + "/login-data.local.json";

    /**
     * Provides all login test data entries from login-data.json.
     * Each entry includes a synthetic "_testCaseName" key with the test case name.
     * Used for data-driven iteration across all scenarios.
     */
    @DataProvider(name = "loginData")
    public Object[][] getLoginData() throws IOException {
        List<Map<String, String>> data = JsonDataReader.readKeyedJsonAsList(LOGIN_DATA_PATH);
        List<Map<String, String>> merged = new ArrayList<>();
        for (Map<String, String> entry : data) {
            merged.add(overlayLocal(entry.get("_testCaseName"), entry));
        }
        Object[][] result = new Object[merged.size()][1];
        for (int i = 0; i < merged.size(); i++) {
            result[i][0] = merged.get(i);
        }
        return result;
    }

    /**
     * Provides a specific login entry by key name.
     * Usage: Tests that need a particular user scenario (e.g. "validUser").
     */
    public static Map<String, String> getLoginEntry(String key) throws IOException {
        return overlayLocal(key, JsonDataReader.readEntry(LOGIN_DATA_PATH, key));
    }

    /**
     * Merges the matching entry from the gitignored local overlay on top of {@code entry}.
     * Returns {@code entry} unchanged when no overlay file or no matching key exists.
     */
    private static Map<String, String> overlayLocal(String key, Map<String, String> entry)
            throws IOException {
        if (key == null || !Files.exists(Path.of(LOGIN_DATA_LOCAL_PATH))) {
            return entry;
        }
        Map<String, String> local = JsonDataReader.readEntry(LOGIN_DATA_LOCAL_PATH, key);
        if (local == null) {
            return entry;
        }
        if (entry == null) {
            return local;
        }
        Map<String, String> merged = new LinkedHashMap<>(entry);
        merged.putAll(local);
        return merged;
    }
}
