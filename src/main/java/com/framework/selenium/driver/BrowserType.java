package com.framework.selenium.driver;

public enum BrowserType {
    CHROME, FIREFOX, EDGE, SAFARI;

    public static BrowserType fromString(String browser) {
        try {
            return BrowserType.valueOf(browser.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported browser: " + browser + ". Supported: chrome, firefox, edge, safari");
        }
    }
}
