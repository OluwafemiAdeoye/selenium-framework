package com.framework.selenium.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * JavaScript execution utilities for DOM interaction, scrolling, and shadow DOM access.
 */
public class JavaScriptHelper {

    private final WebDriver driver;

    public JavaScriptHelper(WebDriver driver) {
        this.driver = driver;
    }

    public Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    public Object executeAsyncScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script, args);
    }

    public void scrollToTop() {
        executeScript("window.scrollTo(0, 0);");
    }

    public void scrollToBottom() {
        executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    public void scrollByPixels(int x, int y) {
        executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    public void scrollToElement(WebElement element) {
        executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                element);
    }

    public void scrollToElement(By locator) {
        scrollToElement(driver.findElement(locator));
    }

    public void click(WebElement element) {
        executeScript("arguments[0].click();", element);
    }

    public void click(By locator) {
        click(driver.findElement(locator));
    }

    public void setAttribute(WebElement element, String attribute, String value) {
        executeScript("arguments[0].setAttribute(arguments[1], arguments[2]);",
                element, attribute, value);
    }

    public void removeAttribute(WebElement element, String attribute) {
        executeScript("arguments[0].removeAttribute(arguments[1]);", element, attribute);
    }

    public String getAttribute(WebElement element, String attribute) {
        return (String) executeScript(
                "return arguments[0].getAttribute(arguments[1]);", element, attribute);
    }

    public String getInnerText(WebElement element) {
        return (String) executeScript("return arguments[0].innerText;", element);
    }

    public void setInnerText(WebElement element, String text) {
        executeScript("arguments[0].innerText = arguments[1];", element, text);
    }

    public void setValue(WebElement element, String value) {
        executeScript("arguments[0].value = arguments[1];", element, value);
    }

    public void highlightElement(WebElement element) {
        executeScript(
                "arguments[0].style.border='3px solid red';", element);
    }

    /**
     * Access the shadow DOM of an element to find nested elements.
     * Returns the shadow root as a SearchContext.
     */
    public SearchContext getShadowRoot(WebElement hostElement) {
        return hostElement.getShadowRoot();
    }

    /**
     * Find an element inside a shadow DOM.
     */
    public WebElement findInShadowRoot(WebElement hostElement, By locator) {
        return hostElement.getShadowRoot().findElement(locator);
    }

    public boolean isPageLoaded() {
        return "complete".equals(executeScript("return document.readyState;"));
    }

    public String getPageTitle() {
        return (String) executeScript("return document.title;");
    }

    public Long getPageHeight() {
        return (Long) executeScript("return document.body.scrollHeight;");
    }

    public void openNewTab(String url) {
        executeScript("window.open(arguments[0], '_blank');", url);
    }
}
