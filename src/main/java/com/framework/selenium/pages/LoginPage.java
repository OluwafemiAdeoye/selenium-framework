package com.framework.selenium.pages;

import com.framework.selenium.config.ConfigManager;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LoginPage extends BasePage {

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("passwordsss");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By errorMessage = By.id("flash");

    @Step("Open login page")
    public LoginPage open() {
        navigateTo(ConfigManager.get("base.url") + "/login");
        return this;
    }

    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        type(usernameField, username);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click login button")
    public HomePage clickLogin() {
        click(loginButton);
        return new HomePage();
    }

    @Step("Login with credentials: {username}")
    public HomePage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        return clickLogin();
    }

    public String getErrorMessage() {
        return getText(errorMessage);
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(errorMessage);
    }
}
