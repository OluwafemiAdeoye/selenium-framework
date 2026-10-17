package com.framework.selenium.pages;

import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.driver.DriverManager;
import com.framework.selenium.utils.ElementHelper;
import com.framework.selenium.utils.JavaScriptHelper;
import com.framework.selenium.utils.WaitHelper;
import com.framework.selenium.utils.WindowHelper;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * Base class for all Page Objects.
 * Provides common interaction methods, logging, and composed helper utilities.
 *
 * <p>A configurable slow-motion delay ({@code step.delay.millis}) is applied
 * after every user-facing interaction (click, type, navigate) so that a human
 * observer can follow the test visually. Set to {@code 0} for full-speed
 * execution in CI.</p>
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WaitHelper waitHelper;
    protected final ElementHelper elementHelper;
    protected final WindowHelper windowHelper;
    protected final JavaScriptHelper jsHelper;

    /** Milliseconds to pause after each interaction so the executor can see the action. */
    private final int stepDelayMillis;

    protected BasePage() {
        PageFactory.initElements(getDriver(), this);
        this.waitHelper = new WaitHelper(getDriver());
        this.elementHelper = new ElementHelper(getDriver());
        this.windowHelper = new WindowHelper(getDriver());
        this.jsHelper = new JavaScriptHelper(getDriver());
        this.stepDelayMillis = ConfigManager.getInt("step.delay.millis", 0);
    }

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Pauses for the configured {@code step.delay.millis} duration.
     * Called after every visible interaction so a human watching can
     * follow the test step by step.
     */
    private void applyStepDelay() {
        if (stepDelayMillis > 0) {
            try {
                Thread.sleep(stepDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Step("Navigate to URL: {url}")
    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        getDriver().get(url);
        applyStepDelay();
    }

    public String getPageTitle() {
        return getDriver().getTitle();
    }

    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    protected void click(By locator) {
        log.debug("Clicking element: {}", locator);
        waitHelper.waitForClickable(locator).click();
        applyStepDelay();
    }

    protected void type(By locator, String text) {
        log.debug("Typing '{}' into element: {}", text, locator);
        WebElement element = waitHelper.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
        applyStepDelay();
    }

    protected String getText(By locator) {
        return waitHelper.waitForVisible(locator).getText();
    }

    protected String getAttribute(By locator, String attribute) {
        return waitHelper.waitForPresence(locator).getAttribute(attribute);
    }

    protected boolean isDisplayed(By locator) {
        return elementHelper.isElementDisplayed(locator);
    }

    protected boolean isEnabled(By locator) {
        return elementHelper.isElementEnabled(locator);
    }

    protected List<WebElement> getElements(By locator) {
        return elementHelper.findElements(locator);
    }

    protected int getElementCount(By locator) {
        return elementHelper.getElementCount(locator);
    }
}
