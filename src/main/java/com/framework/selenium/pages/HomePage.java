package com.framework.selenium.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class HomePage extends BasePage {

    private final By flashMessage = By.id("flash");
    private final By logoutButton = By.cssSelector("a[href='/logout']");
    private final By heading = By.tagName("h4");

    @Step("Get flash message text")
    public String getFlashMessage() {
        return getText(flashMessage);
    }

    public boolean isFlashMessageDisplayed() {
        return isDisplayed(flashMessage);
    }

    @Step("Get heading text")
    public String getHeading() {
        return getText(heading);
    }

    @Step("Click logout")
    public LoginPage logout() {
        click(logoutButton);
        return new LoginPage();
    }

    public boolean isLogoutButtonDisplayed() {
        return isDisplayed(logoutButton);
    }
}
