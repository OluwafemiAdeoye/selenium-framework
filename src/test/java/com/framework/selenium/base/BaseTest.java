package com.framework.selenium.base;

import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.constants.FrameworkConstants;
import com.framework.selenium.driver.DriverFactory;
import com.framework.selenium.driver.DriverManager;
import com.framework.selenium.utils.ScreenshotUtil;
import com.framework.selenium.utils.VideoRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base test class providing driver lifecycle management.
 * Each @Test method gets a fresh browser instance.
 *
 * <p>Uses {@code ThreadLocal} for the video recorder to remain safe when
 * parallel execution is enabled via {@code -Dparallel=methods}.</p>
 *
 * Listeners are registered via testng.xml only (not @Listeners) to avoid double registration.
 */
public abstract class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());
    private static final ThreadLocal<VideoRecorder> VIDEO_RECORDER = new ThreadLocal<>();

    @BeforeSuite(alwaysRun = true)
    public void suiteSetUp() {
        log.info("============================================================");
        log.info("Suite starting — Browser: {}, Env: {}, Mode: {}",
                ConfigManager.get("browser", "chrome"),
                ConfigManager.get("env", "local"),
                ConfigManager.get("execution.mode", "local"));
        log.info("============================================================");

        // Optionally delete old reports from previous runs
        if (ConfigManager.getBoolean("delete.old.reports", false)) {
            cleanDirectory(FrameworkConstants.EXTENT_REPORT_DIR);
            cleanDirectory(FrameworkConstants.SCREENSHOTS_DIR);
            cleanDirectory(FrameworkConstants.VIDEOS_DIR);
            log.info("Old reports, screenshots, and videos cleaned up");
        }

        createDirectory(FrameworkConstants.SCREENSHOTS_DIR);
        createDirectory(FrameworkConstants.VIDEOS_DIR);
        createDirectory(FrameworkConstants.EXTENT_REPORT_DIR);
        createDirectory(FrameworkConstants.DOWNLOADS_DIR);
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        log.info("============================================================");
        log.info("Suite finished — Reports available in target/");
        log.info("  Extent:  {}", FrameworkConstants.EXTENT_REPORT_DIR);
        log.info("  Allure:  {}", FrameworkConstants.ALLURE_RESULTS_DIR);
        log.info("============================================================");
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        log.info("Starting test: {}.{}",
                result.getTestClass().getName(),
                result.getMethod().getMethodName());

        WebDriver driver = DriverFactory.createDriver();
        DriverManager.setDriver(driver);

        boolean recordVideo = ConfigManager.getBoolean("video.record.on.failure", false);
        if (recordVideo) {
            VideoRecorder recorder = new VideoRecorder();
            recorder.startRecording(result.getMethod().getMethodName());
            VIDEO_RECORDER.set(recorder);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            String testName = result.getMethod().getMethodName();

            // Capture screenshot on failure (controlled via config)
            if (result.getStatus() == ITestResult.FAILURE && DriverManager.getDriver() != null) {
                if (ConfigManager.getBoolean("screenshot.on.failure", true)) {
                    ScreenshotUtil.captureScreenshot(testName);
                    log.error("Test failed: {} — screenshot captured", testName);
                } else {
                    log.error("Test failed: {} — screenshot.on.failure is disabled", testName);
                }
            }

            // Capture screenshot on pass (if enabled — useful for visual evidence)
            if (result.getStatus() == ITestResult.SUCCESS && DriverManager.getDriver() != null) {
                if (ConfigManager.getBoolean("screenshot.on.pass", false)) {
                    ScreenshotUtil.captureScreenshot(testName);
                    log.info("Test passed: {} — screenshot captured (screenshot.on.pass enabled)", testName);
                }
            }

            VideoRecorder recorder = VIDEO_RECORDER.get();
            if (recorder != null) {
                recorder.stopRecording(result.getStatus() == ITestResult.FAILURE);
                VIDEO_RECORDER.remove();
            }
        } finally {
            DriverManager.quitDriver();
            log.info("Finished test: {} — Status: {}",
                    result.getMethod().getMethodName(),
                    getStatusName(result.getStatus()));
        }
    }

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    private void createDirectory(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            log.warn("Could not create directory: {}", dir, e);
        }
    }

    /**
     * Deletes all files inside a directory (but not the directory itself).
     */
    private void cleanDirectory(String dir) {
        Path dirPath = Paths.get(dir);
        if (!Files.isDirectory(dirPath)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    Files.delete(entry);
                }
            }
        } catch (IOException e) {
            log.warn("Could not clean directory: {}", dir, e);
        }
    }

    private String getStatusName(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP -> "SKIPPED";
            default -> "UNKNOWN";
        };
    }
}
