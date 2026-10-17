package com.framework.selenium.driver;

import com.framework.selenium.config.ConfigManager;
import com.framework.selenium.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates WebDriver instances based on configuration.
 * Supports local execution and remote execution on cloud platforms
 * (BrowserStack, SauceLabs, LambdaTest).
 *
 * <h3>Execution modes</h3>
 * <ul>
 *   <li><b>Local (default):</b> {@code mvn clean test} — uses local browser</li>
 *   <li><b>Cloud:</b> {@code mvn clean test -Denv=lambdatest -DLT_USERNAME=... -DLT_ACCESS_KEY=...}</li>
 *   <li><b>Pipeline:</b> configured in bitbucket-pipelines.yml / GitHub Actions with env vars</li>
 * </ul>
 *
 * <p>All cloud capabilities are fully config-driven from the platform-specific properties files
 * (e.g. {@code config-lambdatest.properties}). Every setting (video, network, console, tunnel,
 * timeouts, project/build/test names) can be controlled from config or overridden via {@code -D} flags.
 *
 * <p>Credentials are resolved in this order:
 * <ol>
 *   <li>System properties ({@code -DLT_USERNAME}, {@code -DBROWSERSTACK_USERNAME})</li>
 *   <li>Environment variables ({@code LT_USERNAME}, {@code BROWSERSTACK_USERNAME})</li>
 *   <li>Config properties ({@code lambdatest.username}, {@code browserstack.username})</li>
 * </ol>
 */
public final class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver createDriver() {
        String executionMode = ConfigManager.get("execution.mode", "local").toLowerCase();
        String browser = ConfigManager.get("browser", "chrome");
        boolean headless = ConfigManager.getBoolean("headless", false);
        long implicitWait = ConfigManager.getInt("implicit.wait.seconds", 10);
        long pageLoadTimeout = ConfigManager.getInt("page.load.timeout.seconds", 30);
        long scriptTimeout = ConfigManager.getInt("script.timeout.seconds", 30);

        WebDriver driver = switch (executionMode) {
            case "browserstack" -> createBrowserStackDriver(browser);
            case "saucelabs" -> createSauceLabsDriver(browser);
            case "lambdatest" -> createLambdaTestDriver(browser);
            default -> createLocalDriver(browser, headless);
        };

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeout));

        // Window sizing: maximize or set custom dimensions
        if (ConfigManager.getBoolean("window.maximize", true)) {
            driver.manage().window().maximize();
        } else {
            int width = ConfigManager.getInt("window.width", 1920);
            int height = ConfigManager.getInt("window.height", 1080);
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(width, height));
        }

        log.info("Driver created — mode: {}, browser: {}", executionMode, browser);
        return driver;
    }

    // ---- Build & test name helpers ----

    /**
     * Returns the build name for cloud dashboards. Uses the config property
     * ({@code platform.build.name}) first, then falls back to auto-generated
     * "{prefix} Build #{number}" format.
     *
     * <p>The build number is resolved from:
     * <ol>
     *   <li>{@code -DBUILD_NUMBER} or {@code BUILD_NUMBER} env var (CI pipelines)</li>
     *   <li>Timestamp fallback for local runs</li>
     * </ol>
     */
    private static String buildName(String platformPrefix, String configKey) {
        // Check if user provided a custom build name in config
        String configBuildName = ConfigManager.get(configKey);
        if (configBuildName != null && !configBuildName.isBlank() && !configBuildName.contains("${")) {
            return configBuildName;
        }

        // Auto-generate: "PlatformPrefix Build #<number>"
        String buildNumber = resolveCredential("BUILD_NUMBER", "build.number");
        if (buildNumber == null || buildNumber.isBlank()) {
            buildNumber = "local-" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        }
        return platformPrefix + " Build #" + buildNumber;
    }

    /**
     * Resolves the test/session name for cloud dashboards.
     * Falls back to the current thread name if not configured.
     */
    private static String resolveTestName(String configKey) {
        String testName = ConfigManager.get(configKey);
        if (testName != null && !testName.isBlank() && !testName.contains("${")) {
            return testName;
        }
        return Thread.currentThread().getName();
    }

    // ---- Credential resolution helpers ----

    /**
     * Resolves a value by checking:
     * <ol>
     *   <li>System property ({@code -Dkey=value})</li>
     *   <li>Environment variable ({@code export KEY=value})</li>
     *   <li>Config property from properties files</li>
     * </ol>
     *
     * @param envOrSysPropKey the system property / environment variable name (e.g. "LT_USERNAME")
     * @param configKey       the config.properties key (e.g. "lambdatest.username")
     * @return resolved value, or {@code null} if not found anywhere
     */
    private static String resolveCredential(String envOrSysPropKey, String configKey) {
        // 1. System property (-Dkey=value)
        String value = System.getProperty(envOrSysPropKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        // 2. Environment variable
        value = System.getenv(envOrSysPropKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        // 3. Config property (skip if it still contains unresolved ${...} placeholders)
        value = ConfigManager.get(configKey);
        if (value != null && !value.contains("${")) {
            return value;
        }
        return null;
    }

    // ---- Local drivers ----

    private static WebDriver createLocalDriver(String browser, boolean headless) {
        return switch (BrowserType.fromString(browser)) {
            case CHROME -> createChromeDriver(headless);
            case FIREFOX -> createFirefoxDriver(headless);
            case EDGE -> createEdgeDriver(headless);
            case SAFARI -> createSafariDriver();
        };
    }

    private static WebDriver createChromeDriver(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");

        String downloadDir = Path.of(ConfigManager.get("download.dir",
                FrameworkConstants.DOWNLOADS_DIR)).toAbsolutePath().toString();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }

        String downloadDir = Path.of(ConfigManager.get("download.dir",
                FrameworkConstants.DOWNLOADS_DIR)).toAbsolutePath().toString();
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", downloadDir);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/pdf,application/octet-stream,text/csv,application/zip");
        options.setProfile(profile);

        return new FirefoxDriver(options);
    }

    private static WebDriver createEdgeDriver(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }

        String downloadDir = Path.of(ConfigManager.get("download.dir",
                FrameworkConstants.DOWNLOADS_DIR)).toAbsolutePath().toString();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        options.setExperimentalOption("prefs", prefs);

        return new EdgeDriver(options);
    }

    private static WebDriver createSafariDriver() {
        return new SafariDriver(new SafariOptions());
    }

    // ---- Cloud platform drivers ----

    private static WebDriver createBrowserStackDriver(String browser) {
        String username = resolveCredential("BROWSERSTACK_USERNAME", "browserstack.username");
        String accessKey = resolveCredential("BROWSERSTACK_ACCESS_KEY", "browserstack.accesskey");

        // Allow full grid URL override from config; otherwise build from credentials
        String hubUrl = ConfigManager.get("browserstack.grid.url");
        if (hubUrl == null || hubUrl.contains("${")) {
            hubUrl = String.format("https://%s:%s@hub-cloud.browserstack.com/wd/hub",
                    username, accessKey);
        }

        MutableCapabilities capabilities = getCloudBrowserOptions(browser);

        Map<String, Object> bstackOptions = new HashMap<>();
        // Platform
        bstackOptions.put("os", ConfigManager.get("browserstack.os", "Windows"));
        bstackOptions.put("osVersion", ConfigManager.get("browserstack.os.version", "11"));
        bstackOptions.put("browserVersion", ConfigManager.get("browserstack.browser.version", "latest"));
        // Identity
        bstackOptions.put("projectName", ConfigManager.get("browserstack.project.name",
                ConfigManager.get("app.name", "Selenium Framework")));
        bstackOptions.put("buildName", buildName("BrowserStack", "browserstack.build.name"));
        bstackOptions.put("sessionName", resolveTestName("browserstack.test.name"));
        // Diagnostics
        bstackOptions.put("networkLogs", ConfigManager.getBoolean("browserstack.network.logs", true));
        bstackOptions.put("consoleLogs", ConfigManager.get("browserstack.console.logs", "info"));
        bstackOptions.put("video", ConfigManager.getBoolean("browserstack.video", true));
        bstackOptions.put("debug", ConfigManager.getBoolean("browserstack.visual", true));
        bstackOptions.put("seleniumLogs", ConfigManager.getBoolean("browserstack.selenium.logs", true));
        // Tunnel
        bstackOptions.put("local", ConfigManager.get("browserstack.local", "false"));
        String bsLocalId = ConfigManager.get("browserstack.local.identifier", "");
        if (!bsLocalId.isEmpty()) {
            bstackOptions.put("localIdentifier", bsLocalId);
        }
        // Timeouts
        bstackOptions.put("idleTimeout", ConfigManager.getInt("browserstack.idle.timeout", 120));

        capabilities.setCapability("bstack:options", bstackOptions);
        return createRemoteDriver(hubUrl, capabilities);
    }

    private static WebDriver createSauceLabsDriver(String browser) {
        String username = resolveCredential("SAUCELABS_USERNAME", "saucelabs.username");
        if (username == null) {
            username = resolveCredential("SAUCE_USERNAME", "saucelabs.username");
        }
        String accessKey = resolveCredential("SAUCELABS_ACCESS_KEY", "saucelabs.accesskey");
        if (accessKey == null) {
            accessKey = resolveCredential("SAUCE_ACCESS_KEY", "saucelabs.accesskey");
        }

        // Allow full grid URL override from config; otherwise build from credentials + datacenter
        String hubUrl = ConfigManager.get("saucelabs.grid.url");
        if (hubUrl == null || hubUrl.contains("${")) {
            String dataCenter = ConfigManager.get("saucelabs.datacenter", "us-west-1");
            hubUrl = String.format("https://%s:%s@ondemand.%s.saucelabs.com:443/wd/hub",
                    username, accessKey, dataCenter);
        }

        MutableCapabilities capabilities = getCloudBrowserOptions(browser);

        Map<String, Object> sauceOptions = new HashMap<>();
        // Identity
        sauceOptions.put("build", buildName("SauceLabs", "saucelabs.build.name"));
        sauceOptions.put("name", resolveTestName("saucelabs.test.name"));
        // Platform
        sauceOptions.put("platformName", ConfigManager.get("saucelabs.platform", "Windows 11"));
        sauceOptions.put("browserVersion", ConfigManager.get("saucelabs.browser.version", "latest"));
        // Diagnostics
        sauceOptions.put("extendedDebugging", ConfigManager.getBoolean("saucelabs.extended.debugging", true));
        sauceOptions.put("recordVideo", ConfigManager.getBoolean("saucelabs.video", true));
        sauceOptions.put("recordScreenshots", ConfigManager.getBoolean("saucelabs.screenshots", true));
        sauceOptions.put("recordLogs", ConfigManager.getBoolean("saucelabs.selenium.logs", true));
        // Tunnel
        boolean tunnelEnabled = ConfigManager.getBoolean("saucelabs.tunnel", false);
        if (tunnelEnabled) {
            sauceOptions.put("tunnelIdentifier",
                    ConfigManager.get("saucelabs.tunnel.identifier", ""));
        }
        // Timeouts
        sauceOptions.put("idleTimeout", ConfigManager.getInt("saucelabs.idle.timeout", 120));
        sauceOptions.put("maxDuration", ConfigManager.getInt("saucelabs.max.duration", 1800));

        capabilities.setCapability("sauce:options", sauceOptions);
        return createRemoteDriver(hubUrl, capabilities);
    }

    private static WebDriver createLambdaTestDriver(String browser) {
        String username = resolveCredential("LT_USERNAME", "lambdatest.username");
        String accessKey = resolveCredential("LT_ACCESS_KEY", "lambdatest.accesskey");

        // Allow full grid URL override from config; otherwise build from credentials
        String hubUrl = ConfigManager.get("lambdatest.grid.url");
        if (hubUrl == null || hubUrl.contains("${")) {
            hubUrl = String.format("https://%s:%s@hub.lambdatest.com/wd/hub",
                    username, accessKey);
        }

        MutableCapabilities capabilities = getCloudBrowserOptions(browser);

        Map<String, Object> ltOptions = new HashMap<>();
        // Identity
        ltOptions.put("project", ConfigManager.get("lambdatest.project.name",
                ConfigManager.get("app.name", "Selenium Framework")));
        ltOptions.put("build", buildName("LambdaTest", "lambdatest.build.name"));
        ltOptions.put("name", resolveTestName("lambdatest.test.name"));
        // Platform
        ltOptions.put("platformName", ConfigManager.get("lambdatest.platform", "Windows 11"));
        ltOptions.put("browserVersion", ConfigManager.get("lambdatest.browser.version", "latest"));
        // Protocol
        ltOptions.put("w3c", true);
        ltOptions.put("selenium_version", ConfigManager.get("lambdatest.selenium.version", "4.0.0"));
        // Diagnostics
        ltOptions.put("visual", ConfigManager.getBoolean("lambdatest.visual", true));
        ltOptions.put("video", ConfigManager.getBoolean("lambdatest.video", true));
        ltOptions.put("console", ConfigManager.getBoolean("lambdatest.console", true));
        ltOptions.put("network", ConfigManager.getBoolean("lambdatest.network", true));
        // Tunnel
        boolean tunnelEnabled = ConfigManager.getBoolean("lambdatest.tunnel", false);
        ltOptions.put("tunnel", tunnelEnabled);
        if (tunnelEnabled) {
            String tunnelName = ConfigManager.get("lambdatest.tunnel.name", "");
            if (!tunnelName.isEmpty()) {
                ltOptions.put("tunnelName", tunnelName);
            }
        }
        // Timeouts
        ltOptions.put("idleTimeout", ConfigManager.getInt("lambdatest.idle.timeout", 120));

        capabilities.setCapability("LT:Options", ltOptions);
        return createRemoteDriver(hubUrl, capabilities);
    }

    /**
     * Returns browser options suitable for cloud platforms.
     * Cloud drivers don't need local download prefs or headless flags —
     * those are controlled by the platform.
     */
    private static MutableCapabilities getCloudBrowserOptions(String browser) {
        return switch (BrowserType.fromString(browser)) {
            case CHROME -> new ChromeOptions();
            case FIREFOX -> new FirefoxOptions();
            case EDGE -> new EdgeOptions();
            case SAFARI -> new SafariOptions();
        };
    }

    private static WebDriver createRemoteDriver(String hubUrl, MutableCapabilities capabilities) {
        try {
            log.info("Connecting to remote hub: {}", hubUrl.replaceAll(":[^:@]+@", ":***@"));
            return new RemoteWebDriver(new URL(hubUrl), capabilities);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid remote hub URL: " + hubUrl, e);
        }
    }
}
