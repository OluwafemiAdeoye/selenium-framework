package com.framework.selenium.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.constants.FrameworkConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe ExtentReports manager.
 * Uses ThreadLocal for ExtentTest to support parallel execution.
 * Report theme, title, and name are configurable via config.properties.
 */
public final class ExtentReportManager {

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> EXTENT_TEST = new ThreadLocal<>();

    private ExtentReportManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extentReports == null) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String reportPath = FrameworkConstants.EXTENT_REPORT_DIR
                    + "/ExtentReport_" + timestamp + ".html";

            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);

            // Theme from config: STANDARD or DARK
            String themeName = ConfigManager.get("extent.report.theme", "STANDARD").toUpperCase();
            Theme theme = "DARK".equals(themeName) ? Theme.DARK : Theme.STANDARD;
            sparkReporter.config().setTheme(theme);

            // Title and report name from config
            sparkReporter.config().setDocumentTitle(
                    ConfigManager.get("extent.report.title", "Automation Test Report"));
            sparkReporter.config().setReportName(
                    ConfigManager.get("extent.report.name", "Selenium Framework Report"));
            sparkReporter.config().setTimelineEnabled(true);

            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);

            // System info for reports
            extentReports.setSystemInfo("OS", System.getProperty("os.name"));
            extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
            extentReports.setSystemInfo("Browser", ConfigManager.get("browser", "chrome"));
            extentReports.setSystemInfo("Execution Mode", ConfigManager.get("execution.mode", "local"));
            extentReports.setSystemInfo("Environment", ConfigManager.get("env", "local"));
        }
        return extentReports;
    }

    public static void setTest(ExtentTest test) {
        EXTENT_TEST.set(test);
    }

    public static ExtentTest getTest() {
        return EXTENT_TEST.get();
    }

    public static void removeTest() {
        EXTENT_TEST.remove();
    }

    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
