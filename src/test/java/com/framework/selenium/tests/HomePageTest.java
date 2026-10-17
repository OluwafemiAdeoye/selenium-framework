package com.framework.selenium.tests;

import com.framework.selenium.base.BaseTest;
import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.dataproviders.JsonDataProvider;
import com.framework.selenium.pages.HomePage;
import com.framework.selenium.pages.LoginPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

@Epic("User Authentication")
@Feature("Home Page")
public class HomePageTest extends BaseTest {

    private static final ThreadLocal<HomePage> HOME_PAGE = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void navigateToHomePage() throws IOException {
        Map<String, String> validUser = JsonDataProvider.getLoginEntry("validUser");
        LoginPage loginPage = new LoginPage();
        loginPage.navigateTo(ConfigManager.get("base.url") + "/login");
        HOME_PAGE.set(loginPage.loginAs(validUser.get("username"), validUser.get("password")));
    }

    @Test(description = "Verify secure area page heading",
            groups = {"smoke"})
    @Severity(SeverityLevel.NORMAL)
    @Story("Page Content")
    public void testSecureAreaHeading() {
        String heading = HOME_PAGE.get().getHeading();
        Assert.assertNotNull(heading, "Heading should not be null");
    }

    @Test(description = "Verify logout functionality",
            groups = {"smoke", "regression"})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Logout")
    public void testLogout() {
        LoginPage loginPage = HOME_PAGE.get().logout();
        HOME_PAGE.remove();
        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Flash message should appear after logout");
    }
}
