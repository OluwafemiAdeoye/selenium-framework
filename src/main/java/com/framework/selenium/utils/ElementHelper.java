package com.framework.selenium.utils;

import com.framework.selenium.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Higher-level element interaction utilities.
 * Supports optional element highlighting for debugging (controlled via config).
 */
public class ElementHelper {

    private final WebDriver driver;
    private final boolean highlightEnabled;

    public ElementHelper(WebDriver driver) {
        this.driver = driver;
        this.highlightEnabled = ConfigManager.getBoolean("highlight.elements", false);
    }

    // ---- Element state checks ----

    public boolean isElementDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    public boolean isElementEnabled(By locator) {
        try {
            return driver.findElement(locator).isEnabled();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    public boolean isElementSelected(By locator) {
        try {
            return driver.findElement(locator).isSelected();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    // ---- Select / Dropdown ----

    public void selectByVisibleText(By locator, String text) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        new Select(element).selectByVisibleText(text);
    }

    public void selectByValue(By locator, String value) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        new Select(element).selectByValue(value);
    }

    public void selectByIndex(By locator, int index) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        new Select(element).selectByIndex(index);
    }

    public String getSelectedText(By locator) {
        return new Select(driver.findElement(locator)).getFirstSelectedOption().getText();
    }

    // ---- Actions ----

    public void hoverOver(By locator) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        new Actions(driver).moveToElement(element).perform();
    }

    public void doubleClick(By locator) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        new Actions(driver).doubleClick(element).perform();
    }

    public void rightClick(By locator) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        new Actions(driver).contextClick(element).perform();
    }

    public void dragAndDrop(By source, By target) {
        WebElement sourceEl = driver.findElement(source);
        WebElement targetEl = driver.findElement(target);
        highlightElement(sourceEl);
        highlightElement(targetEl);
        new Actions(driver).dragAndDrop(sourceEl, targetEl).perform();
    }

    // ---- JavaScript helpers ----

    public void scrollToElement(By locator) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
    }

    public void jsClick(By locator) {
        WebElement element = driver.findElement(locator);
        highlightElement(element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    // ---- Attribute / CSS ----

    public String getAttribute(By locator, String attribute) {
        return driver.findElement(locator).getAttribute(attribute);
    }

    public String getCssValue(By locator, String propertyName) {
        return driver.findElement(locator).getCssValue(propertyName);
    }

    // ---- Collection helpers ----

    public List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    public int getElementCount(By locator) {
        return driver.findElements(locator).size();
    }

    // ---- Highlight helper ----

    /**
     * Briefly highlights an element with a red border for debugging visibility.
     * Controlled by the {@code highlight.elements} config property.
     */
    private void highlightElement(WebElement element) {
        if (!highlightEnabled) {
            return;
        }
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String originalStyle = element.getAttribute("style");
            js.executeScript(
                    "arguments[0].setAttribute('style', arguments[1]);",
                    element,
                    "border: 2px solid red; background: lightyellow;" + (originalStyle != null ? originalStyle : ""));

            Thread.sleep(200);

            js.executeScript(
                    "arguments[0].setAttribute('style', arguments[1]);",
                    element,
                    originalStyle != null ? originalStyle : "");
        } catch (Exception ignored) {
            // Highlighting is best-effort — don't break tests if it fails
        }
    }
}
