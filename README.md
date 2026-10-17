# Selenium Automation Framework

A Page Object Model test automation framework built on Selenium 4 and TestNG, with layered
configuration, thread-safe parallel execution, dual reporting, and local or cloud-grid execution.

![Java](https://img.shields.io/badge/Java-21-orange)
![Selenium](https://img.shields.io/badge/Selenium-4.27.0-green)
![TestNG](https://img.shields.io/badge/TestNG-7.10.2-blue)
![Maven](https://img.shields.io/badge/build-Maven-red)

## Features

- **Page Object Model** — page classes expose intent (`loginAs`, `logout`), not locators
- **Thread-safe driver management** — `ThreadLocal` WebDriver, one fresh browser per `@Test`
- **Layered configuration** — system properties override per-environment files, which override base defaults
- **Local and cloud execution** — Chrome, Firefox, Edge, Safari locally; BrowserStack, SauceLabs, or LambdaTest remotely
- **Parallel execution** — driven from `config.properties`, applied at runtime via a TestNG `IAlterSuiteListener`
- **Dual reporting** — Allure and ExtentReports, with screenshots and video attached to failures
- **Data-driven tests** — JSON and CSV data providers
- **Automatic retry** — flaky tests retried via `RetryAnalyzer`, wired in by an annotation transformer
- **No committed credentials** — supplied by environment variables or a gitignored overlay file
- **CI ready** — GitHub Actions and Bitbucket Pipelines definitions included

## Requirements

| | |
|---|---|
| Java | 21 |
| Maven | 3.8+ |
| Browser | Chrome, Firefox, Edge, or Safari installed locally |

Browser drivers resolve automatically via Selenium Manager, built into Selenium 4 — nothing to
download or put on your `PATH`.

## Quick start

```bash
git clone git@github.com:OluwafemiAdeoye/selenium-framework.git
cd selenium-framework
mvn clean test-compile     # verify the toolchain before running a browser
```

A fresh clone needs two things pointed at your own application before the suite will pass: a target
URL and a set of credentials. Neither is committed.

**1. Target URL.** `base.url` in `src/main/resources/config/config.properties` carries the default,
or override it per run:

```bash
mvn clean test -Dbase.url=https://your-app.example.com
```

The bundled page objects and test data are written against the
[the-internet.herokuapp.com](https://the-internet.herokuapp.com) login demo, whose form uses
`#username`, `#password`, and a `button[type='submit']`. If your application's login differs, update
the locators in `src/main/java/com/framework/selenium/pages/LoginPage.java`.

**2. Credentials.** See [Supplying test credentials](#supplying-test-credentials) below. The quickest
route:

```bash
mvn clean test -Dbase.url=https://the-internet.herokuapp.com \
  -DTEST_USER_EMAIL=tomsmith -DTEST_USER_PASSWORD='SuperSecretPassword!'
```

That pair is the demo site's own published credentials, which makes it a convenient smoke check that
the framework itself is wired up correctly — it runs the smoke suite green in about a minute.

> If you have already created a `login-data.local.json`, its `validUser` entry takes precedence over
> these `-D` flags and the check will fail against the demo site. Move that file aside first.

## Supplying test credentials

Credentials are never committed. `login-data.json` ships with `${NAME}` placeholders that resolve at
runtime, so pick whichever route suits you.

**Environment variables** — best for CI:

```bash
export TEST_USER_EMAIL='user@example.com'
export TEST_USER_PASSWORD='your-password'
export TEST_USER_OTP='123456'
mvn clean test
```

System properties work too: `-DTEST_USER_EMAIL=...`.

**Local overlay file** — best for day-to-day local runs. Create
`src/test/resources/testdata/login-data.local.json` (already gitignored):

```json
{
  "validUser": {
    "username": "user@example.com",
    "password": "your-password",
    "otp": "123456"
  }
}
```

Fields found here override the same fields in the committed `login-data.json`, so you only list what
you want to replace. Resolution order is **local overlay → system property → environment variable**.

> A placeholder that resolves to nothing is deliberately left as the literal `${TEST_USER_EMAIL}`
> rather than becoming an empty string, so a missing credential fails visibly instead of silently
> submitting a blank form.

## Running tests

### Suites

Each suite is a Maven profile bound to a TestNG XML file.

```bash
mvn clean test              # Full suite  — testng.xml
mvn clean test -Psmoke      # Smoke       — testng-smoke.xml       (group: smoke)
mvn clean test -Pregression # Regression  — testng-regression.xml  (group: regression)
```

### Browser and display

```bash
mvn clean test -Dbrowser=firefox
mvn clean test -Dheadless=true
mvn clean test -Dbrowser=edge -Dheadless=true
```

Valid browsers: `chrome` (default), `firefox`, `edge`, `safari`.

### Parallel execution

Configured in `config.properties` and read at runtime — no Maven flags required:

```properties
parallel.execution=true
parallel.mode=methods    # methods | classes | tests
thread.count=2
```

Overridable per run:

```bash
mvn clean test -Dparallel.execution=true -Dparallel.mode=methods -Dthread.count=4
```

When targeting a cloud grid, keep `thread.count` within your plan's concurrent-session limit.

### Environments

```bash
mvn clean test -Denv=staging
mvn clean test -Denv=production
```

`-Denv=X` overlays `config-X.properties` on top of `config.properties`. Add a new environment by
dropping another file into `src/main/resources/config/`.

### Debugging aids

```properties
highlight.elements=true    # outline each element before interacting with it
step.delay.millis=500      # slow each interaction so a human can follow along
```

## Cloud grid execution

```bash
mvn clean test -Denv=lambdatest    -DLT_USERNAME=user -DLT_ACCESS_KEY=key
mvn clean test -Denv=browserstack  -DBROWSERSTACK_USERNAME=user -DBROWSERSTACK_ACCESS_KEY=key
mvn clean test -Denv=saucelabs     -DSAUCE_USERNAME=user -DSAUCE_ACCESS_KEY=key
```

Credentials resolve in order: **system property → environment variable → config file**. Config files
hold only `${...}` placeholders, and an unresolved placeholder is skipped so resolution falls through
to the next tier. The grid URL is assembled from the resolved credentials.

Per-platform settings — video, console and network logs, tunnels, timeouts — live in
`config-lambdatest.properties`, `config-browserstack.properties`, and `config-saucelabs.properties`,
and any of them can be overridden with a `-D` flag:

```bash
mvn clean test -Denv=lambdatest -Dlambdatest.video=false -Dlambdatest.tunnel=true
```

## Reports

| Report | Location | Command |
|---|---|---|
| Allure | `target/allure-report/index.html` | generated automatically; `mvn allure:serve` to open |
| ExtentReports | `target/extent-reports/` | generated automatically |
| Screenshots | `target/screenshots/` | on failure by default |
| Video | `target/videos/` | on failure, local headed runs only |
| Logs | `target/logs/framework.log` | Log4j2, rolled daily |

Relevant toggles in `config.properties`:

```properties
screenshot.on.failure=true
screenshot.on.pass=false
video.record.on.failure=true
delete.old.reports=false
extent.report.theme=DARK
```

Video recording uses a screen recorder, so it needs a real display — it is a no-op in headless and
cloud runs, where the grid's own recording takes over instead.

## Project structure

```
src/
├── main/java/com/framework/selenium/
│   ├── config/        ConfigManager, EnvironmentType — layered property resolution
│   ├── constants/     FrameworkConstants — output paths
│   ├── driver/        DriverFactory, DriverManager, BrowserType — local + cloud drivers
│   ├── listeners/     TestListener, RetryAnalyzer, RetryTransformer, ParallelConfigListener
│   ├── pages/         BasePage, LoginPage, HomePage — page objects
│   ├── reporting/     ExtentReportManager
│   └── utils/         Wait, element, JS, window, screenshot, video, download, data-reader helpers
├── main/resources/
│   ├── config/        config.properties + per-environment overlays
│   └── log4j2.xml
└── test/
    ├── java/com/framework/selenium/
    │   ├── base/              BaseTest — driver lifecycle per test method
    │   ├── dataproviders/     JsonDataProvider, CsvDataProvider
    │   └── tests/             LoginTest, HomePageTest
    └── resources/testdata/    login-data.json, login-data.csv
```

Suite definitions (`testng.xml`, `testng-smoke.xml`, `testng-regression.xml`) sit at the project root.

## Writing a test

Page objects extend `BasePage`, keep locators private, and return the next page so tests read as a
flow. `@Step` annotations surface each action as a named step in the Allure report:

```java
public class LoginPage extends BasePage {

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton = By.cssSelector("button[type='submit']");

    @Step("Open login page")
    public LoginPage open() {
        navigateTo(ConfigManager.get("base.url") + "/login");
        return this;
    }

    @Step("Login with credentials: {username}")
    public HomePage loginAs(String username, String password) {
        type(usernameField, username);
        type(passwordField, password);
        click(loginButton);
        return new HomePage();
    }
}
```

Page objects take no constructor arguments — `BasePage` pulls the current thread's driver from
`DriverManager`, which keeps them safe under parallel execution.

Tests extend `BaseTest`, which supplies a fresh browser per method and captures evidence on failure:

```java
@Epic("User Authentication")
@Feature("Login")
public class LoginTest extends BaseTest {

    @Test(description = "Verify successful login with valid credentials", groups = {"smoke"})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Valid Login")
    public void testSuccessfulLogin() throws IOException {
        Map<String, String> validUser = JsonDataProvider.getLoginEntry("validUser");

        LoginPage loginPage = new LoginPage().open();
        HomePage homePage = loginPage.loginAs(validUser.get("username"), validUser.get("password"));

        Assert.assertTrue(homePage.isLogoutButtonDisplayed(),
                "Logout button should be visible after successful login");
    }
}
```

Tag a test with `groups = {"smoke"}` or `groups = {"regression"}` to include it in those suites. The
`@Epic`, `@Feature`, `@Story`, and `@Severity` annotations are optional, and organize the Allure
report's behaviour view.

### Data-driven tests

```java
@Test(dataProvider = "loginData", dataProviderClass = JsonDataProvider.class,
      groups = {"regression"})
public void testLoginWithJsonData(Map<String, String> testData) {
    String testCaseName = testData.getOrDefault("_testCaseName", "unknown");
    // one invocation per entry in login-data.json
}
```

`JsonDataProvider` adds a synthetic `_testCaseName` key holding each entry's JSON key, which keeps
report output readable. `CsvDataProvider` exposes `loginDataCsv` for `login-data.csv`, yielding one
header-keyed `Map` per row — same shape, so a test can consume either source interchangeably, minus
the `_testCaseName` key.

## Continuous integration

**GitHub Actions** (`.github/workflows/test.yml`) runs on pushes to `main` and `develop`, on pull
requests into `main`, and on manual dispatch — where suite and execution environment are selectable
inputs. Allure reports, raw results, ExtentReports, logs, and failure screenshots upload as
artifacts.

**Bitbucket Pipelines** (`bitbucket-pipelines.yml`) provides an equivalent pipeline.

Cloud credentials come from repository secrets (`LT_USERNAME`, `LT_ACCESS_KEY`,
`BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY`, `SAUCELABS_USERNAME`, `SAUCELABS_ACCESS_KEY`) and
are passed as system properties, never written to a file.

## Configuration reference

Resolution order is **system property (`-Dkey=value`) → `config-{env}.properties` → `config.properties`**.

| Key | Default | Purpose |
|---|---|---|
| `base.url` | — | Application under test |
| `browser` | `chrome` | `chrome`, `firefox`, `edge`, `safari` |
| `headless` | `false` | Run without a visible browser window |
| `execution.mode` | `local` | `local`, `browserstack`, `saucelabs`, `lambdatest` |
| `window.maximize` | `true` | Maximize on launch; else use `window.width`/`window.height` |
| `implicit.wait.seconds` | `10` | Implicit wait |
| `explicit.wait.seconds` | `15` | Default explicit-wait timeout |
| `page.load.timeout.seconds` | `30` | Page load timeout |
| `polling.interval.millis` | `500` | Fluent-wait polling interval |
| `parallel.execution` | `true` | Enable parallel execution |
| `parallel.mode` | `methods` | `methods`, `classes`, `tests` |
| `thread.count` | `2` | Parallel threads |
| `max.retry.count` | `1` | Retries for a failing test |
| `screenshot.on.failure` | `true` | Capture and attach on failure |
| `video.record.on.failure` | `true` | Record video on failure (headed only) |
| `highlight.elements` | `false` | Outline elements before interaction |
| `step.delay.millis` | `500` | Delay between interactions |
| `download.dir` | `target/downloads` | Browser download directory |

## License

Unlicensed — all rights reserved.
