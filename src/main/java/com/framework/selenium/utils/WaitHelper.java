package com.framework.selenium.utils;

import com.framework.selenium.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Centralized explicit wait helper.
 * Replaces scattered WebDriverWait instantiations throughout page objects.
 */
public class WaitHelper {

    private final WebDriver driver;
    private final Duration defaultTimeout;
    private final Duration pollingInterval;

    public WaitHelper(WebDriver driver) {
        this.driver = driver;
        this.defaultTimeout = Duration.ofSeconds(
                ConfigManager.getInt("explicit.wait.seconds", 15));
        this.pollingInterval = Duration.ofMillis(
                ConfigManager.getInt("polling.interval.millis", 500));
    }

    public WebElement waitForVisible(By locator) {
        return createWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisible(By locator, Duration timeout) {
        return createWait(timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return createWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(By locator, Duration timeout) {
        return createWait(timeout).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public boolean waitForInvisibility(By locator) {
        return createWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForUrlContains(String urlFragment) {
        return createWait().until(ExpectedConditions.urlContains(urlFragment));
    }

    public boolean waitForTitleContains(String titleFragment) {
        return createWait().until(ExpectedConditions.titleContains(titleFragment));
    }

    public WebElement waitForPresence(By locator) {
        return createWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public List<WebElement> waitForAllVisible(By locator) {
        return createWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public boolean waitForTextPresent(By locator, String text) {
        return createWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public WebDriver waitForFrameAndSwitch(By locator) {
        return createWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    public boolean waitForAlertPresent() {
        createWait().until(ExpectedConditions.alertIsPresent());
        return true;
    }

    private WebDriverWait createWait() {
        return createWait(defaultTimeout);
    }

    private WebDriverWait createWait(Duration timeout) {
        return new WebDriverWait(driver, timeout, pollingInterval);
    }
}
