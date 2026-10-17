package com.framework.selenium.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles browser window/tab management, popups, and iframe switching.
 */
public class WindowHelper {

    private final WebDriver driver;

    public WindowHelper(WebDriver driver) {
        this.driver = driver;
    }

    public String getCurrentWindowHandle() {
        return driver.getWindowHandle();
    }

    public Set<String> getAllWindowHandles() {
        return driver.getWindowHandles();
    }

    public void switchToWindow(String windowHandle) {
        driver.switchTo().window(windowHandle);
    }

    /**
     * Switches to the most recently opened window/tab.
     */
    public void switchToNewWindow() {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(handles.getLast());
    }

    /**
     * Opens a new tab and switches to it.
     */
    public void openNewTab() {
        driver.switchTo().newWindow(WindowType.TAB);
    }

    /**
     * Opens a new browser window and switches to it.
     */
    public void openNewWindow() {
        driver.switchTo().newWindow(WindowType.WINDOW);
    }

    /**
     * Closes the current window/tab and switches back to the first remaining window.
     */
    public void closeCurrentAndSwitchBack() {
        String currentHandle = driver.getWindowHandle();
        driver.close();
        Set<String> remaining = driver.getWindowHandles();
        if (!remaining.isEmpty()) {
            driver.switchTo().window(remaining.iterator().next());
        }
    }

    /**
     * Closes all windows except the given one and switches to it.
     */
    public void closeAllExcept(String windowHandle) {
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(windowHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(windowHandle);
    }

    public int getWindowCount() {
        return driver.getWindowHandles().size();
    }

    // --- iframe methods ---

    public void switchToFrame(By locator) {
        driver.switchTo().frame(driver.findElement(locator));
    }

    public void switchToFrame(int index) {
        driver.switchTo().frame(index);
    }

    public void switchToFrame(String nameOrId) {
        driver.switchTo().frame(nameOrId);
    }

    public void switchToParentFrame() {
        driver.switchTo().parentFrame();
    }

    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    // --- alert methods ---

    public String getAlertText() {
        return driver.switchTo().alert().getText();
    }

    public void acceptAlert() {
        driver.switchTo().alert().accept();
    }

    public void dismissAlert() {
        driver.switchTo().alert().dismiss();
    }

    public void sendKeysToAlert(String text) {
        driver.switchTo().alert().sendKeys(text);
    }
}
