package com.framework.selenium.utils;

import com.framework.selenium.constants.FrameworkConstants;
import com.framework.selenium.driver.DriverManager;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots and attaches them to both Allure and ExtentReports.
 */
public final class ScreenshotUtil {

    private static final Logger log = LogManager.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ScreenshotUtil() {
    }

    /**
     * Captures a screenshot, saves it to disk, and attaches it to Allure.
     * Returns the file path for reporting integration.
     */
    public static String captureScreenshot(String testName) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("Driver is null, cannot capture screenshot");
            return null;
        }

        try {
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String fileName = testName + "_" + timestamp + ".png";
            Path screenshotDir = Paths.get(FrameworkConstants.SCREENSHOTS_DIR);
            Files.createDirectories(screenshotDir);
            Path filePath = screenshotDir.resolve(fileName);
            Files.write(filePath, screenshotBytes);

            Allure.addAttachment("Screenshot - " + testName,
                    new ByteArrayInputStream(screenshotBytes));

            log.info("Screenshot saved: {}", filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (Exception e) {
            log.error("Failed to capture screenshot for test: {}", testName, e);
            return null;
        }
    }

    /**
     * Returns screenshot as Base64 string (used by ExtentReports).
     */
    public static String captureScreenshotAsBase64() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            return null;
        }
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }
}
