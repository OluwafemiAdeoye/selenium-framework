package com.framework.selenium.dataproviders;

import com.framework.selenium.constants.FrameworkConstants;
import com.framework.selenium.utils.CsvDataReader;
import com.opencsv.exceptions.CsvException;
import org.testng.annotations.DataProvider;

import java.io.IOException;

/**
 * Provides test data from CSV files for data-driven tests.
 * Returns Map&lt;String, String&gt; per row — keyed by column header — so tests
 * can access fields by name (e.g. testData.get("username")) rather than
 * fragile index positions.
 */
public class CsvDataProvider {

    private static final String LOGIN_DATA_PATH =
            FrameworkConstants.TEST_DATA_DIR + "/login-data.csv";

    /**
     * Provides all login test data from login-data.csv as header-mapped Maps.
     * Each Map is keyed by column header: username, password, expectedOutcome, description.
     * Identical format to JsonDataProvider.getLoginData() so tests can consume both interchangeably.
     */
    @DataProvider(name = "loginDataCsv")
    public Object[][] getLoginDataFromCsv() throws IOException, CsvException {
        return CsvDataReader.readCsvAsMapsDataProvider(LOGIN_DATA_PATH);
    }
}
