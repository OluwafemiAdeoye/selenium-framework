package com.framework.selenium.tests;

import com.framework.selenium.base.BaseTest;
import com.framework.selenium.dataproviders.JsonDataProvider;
import com.framework.selenium.pages.HomePage;
import com.framework.selenium.pages.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

@Epic("User Authentication")
@Feature("Login")
public class LoginTest extends BaseTest {

    @Test(description = "Verify successful login with valid credentials",
            groups = {"smoke"})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Valid Login")
    public void testSuccessfulLogin() throws IOException {
        Map<String, String> validUser = JsonDataProvider.getLoginEntry("validUser");
        LoginPage loginPage = new LoginPage().open();
        HomePage homePage = loginPage.loginAs(validUser.get("username"), validUser.get("password"));

        Assert.assertTrue(homePage.isLogoutButtonDisplayed(),
                "Logout button should be visible after successful login");
    }

    @Test(description = "Verify error message for invalid credentials",
            groups = {"smoke", "regression"})
    @Severity(SeverityLevel.NORMAL)
    @Story("Invalid Login")
    public void testInvalidLogin() throws IOException {
        Map<String, String> invalidUser = JsonDataProvider.getLoginEntry("invalidPasswordUser");
        LoginPage loginPage = new LoginPage().open();
        loginPage.enterUsername(invalidUser.get("username"));
        loginPage.enterPassword(invalidUser.get("password"));
        loginPage.clickLogin();

        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should appear for invalid credentials");
    }

    @Test(dataProvider = "loginData", dataProviderClass = JsonDataProvider.class,
            description = "Verify login with multiple data sets from JSON",
            groups = {"regression"})
    @Severity(SeverityLevel.NORMAL)
    @Story("Data-Driven Login")
    public void testLoginWithJsonData(Map<String, String> testData) {
        String testCaseName = testData.getOrDefault("_testCaseName", "unknown");
        log.info("Running data-driven test case: {} — {}", testCaseName,
                testData.getOrDefault("description", ""));

        LoginPage loginPage = new LoginPage().open();
        loginPage.enterUsername(testData.get("username"));
        loginPage.enterPassword(testData.get("password"));
        HomePage homePage = loginPage.clickLogin();

        String expectedOutcome = testData.get("expectedOutcome");
        if ("success".equals(expectedOutcome)) {
            Assert.assertTrue(homePage.isLogoutButtonDisplayed(),
                    "[" + testCaseName + "] Logout button should be visible for valid login");
        } else {
            Assert.assertTrue(loginPage.isErrorDisplayed(),
                    "[" + testCaseName + "] Error should be displayed for invalid login");
        }
    }
}
