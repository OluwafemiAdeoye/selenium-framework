package com.framework.selenium.dataproviders;

import com.framework.selenium.constants.FrameworkConstants;
import com.framework.selenium.utils.JsonDataReader;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Provides test data from JSON files for data-driven tests.
 * Reads the keyed-object JSON format where each entry has a descriptive key.
 */
public class JsonDataProvider {

    private static final String LOGIN_DATA_PATH =
            FrameworkConstants.TEST_DATA_DIR + "/login-data.json";

    /**
     * Provides all login test data entries from login-data.json.
     * Each entry includes a synthetic "_testCaseName" key with the test case name.
     * Used for data-driven iteration across all scenarios.
     */
    @DataProvider(name = "loginData")
    public Object[][] getLoginData() throws IOException {
        List<Map<String, String>> data = JsonDataReader.readKeyedJsonAsList(LOGIN_DATA_PATH);
        Object[][] result = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        return result;
    }

    /**
     * Provides a specific login entry by key name.
     * Usage: Tests that need a particular user scenario (e.g. "validUser").
     */
    public static Map<String, String> getLoginEntry(String key) throws IOException {
        return JsonDataReader.readEntry(LOGIN_DATA_PATH, key);
    }
}
