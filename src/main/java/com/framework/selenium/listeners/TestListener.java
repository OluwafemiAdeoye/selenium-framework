package com.framework.selenium.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.driver.DriverManager;
import com.framework.selenium.reporting.ExtentReportManager;
import com.framework.selenium.utils.ScreenshotUtil;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Unified TestNG listener handling both ExtentReports and Allure reporting.
 * Captures screenshots on failure for both reporting tools.
 * Registered via testng.xml only — NOT via @Listeners on BaseTest to avoid double registration.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    // ---- Suite lifecycle ----

    @Override
    public void onStart(ISuite suite) {
        log.info("Suite started: {}", suite.getName());

        var report = ExtentReportManager.getInstance();
        String executionMode = ConfigManager.get("execution.mode", "local");

        report.setSystemInfo("Browser", ConfigManager.get("browser", "chrome"));
        report.setSystemInfo("Environment", ConfigManager.get("env", "local"));
        report.setSystemInfo("Base URL", ConfigManager.get("base.url", "N/A"));
        report.setSystemInfo("Execution Mode", executionMode);

        // Cloud platform details — only when running remotely
        switch (executionMode.toLowerCase()) {
            case "lambdatest" -> {
                report.setSystemInfo("Platform", ConfigManager.get("lambdatest.platform", "N/A"));
                report.setSystemInfo("Project", ConfigManager.get("lambdatest.project.name", "N/A"));
                report.setSystemInfo("Tunnel", ConfigManager.get("lambdatest.tunnel", "false"));
            }
            case "browserstack" -> {
                report.setSystemInfo("OS", ConfigManager.get("browserstack.os", "N/A")
                        + " " + ConfigManager.get("browserstack.os.version", ""));
                report.setSystemInfo("Project", ConfigManager.get("browserstack.project.name", "N/A"));
                report.setSystemInfo("Tunnel", ConfigManager.get("browserstack.local", "false"));
            }
            case "saucelabs" -> {
                report.setSystemInfo("Platform", ConfigManager.get("saucelabs.platform", "N/A"));
                report.setSystemInfo("Datacenter", ConfigManager.get("saucelabs.datacenter", "N/A"));
                report.setSystemInfo("Tunnel", ConfigManager.get("saucelabs.tunnel", "false"));
            }
            default -> { /* local — no extra info needed */ }
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("Suite finished: {}", suite.getName());
        ExtentReportManager.flushReports();
    }

    // ---- Test lifecycle ----

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getTestClass().getName() + " :: " + result.getMethod().getMethodName();
        log.info("Test started: {}", testName);

        ExtentTest test = ExtentReportManager.getInstance().createTest(
                testName, result.getMethod().getDescription());
        ExtentReportManager.setTest(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("Test passed: {}", result.getMethod().getMethodName());
        ExtentReportManager.getTest().log(Status.PASS, "Test passed");
        ExtentReportManager.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.error("Test failed: {}", testName);

        ExtentTest extentTest = ExtentReportManager.getTest();
        extentTest.log(Status.FAIL, result.getThrowable());

        // Screenshot to disk + Allure attachment is handled in BaseTest @AfterMethod
        // (before the driver is quit). Here we attach to ExtentReports if the driver
        // is still alive and screenshot.on.failure is enabled.
        boolean screenshotEnabled = ConfigManager.getBoolean("screenshot.on.failure", true);
        if (screenshotEnabled && DriverManager.getDriver() != null) {
            String base64 = ScreenshotUtil.captureScreenshotAsBase64();
            if (base64 != null) {
                extentTest.fail("Screenshot on failure",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            }
        }

        if (result.getThrowable() != null) {
            Allure.addAttachment("Failure Reason", result.getThrowable().getMessage());
        }

        ExtentReportManager.removeTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("Test skipped: {}", result.getMethod().getMethodName());

        ExtentTest extentTest = ExtentReportManager.getTest();
        extentTest.log(Status.SKIP, "Test skipped");
        if (result.getThrowable() != null) {
            extentTest.log(Status.SKIP, result.getThrowable());
            Allure.addAttachment("Skip Reason", result.getThrowable().getMessage());
        }

        ExtentReportManager.removeTest();
    }
}
