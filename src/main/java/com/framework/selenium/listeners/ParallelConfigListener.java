package com.framework.selenium.listeners;

import com.framework.selenium.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import java.util.List;

/**
 * Applies parallel execution settings from config.properties to the TestNG suite
 * before any tests run. Uses {@link IAlterSuiteListener} which fires before
 * suite-level listeners, making it the earliest hook to configure parallelism.
 *
 * <p>Config properties read:
 * <ul>
 *     <li>{@code parallel.execution} — true/false (default: false)</li>
 *     <li>{@code parallel.mode} — methods | classes | tests (default: methods)</li>
 *     <li>{@code thread.count} — number of threads (default: 2)</li>
 * </ul>
 *
 * <p>All values can be overridden via {@code -D} flags:
 * {@code mvn clean test -Dparallel.execution=true -Dparallel.mode=methods -Dthread.count=4}
 *
 * <p>Local Safari is forced sequential regardless of these settings — see
 * {@link #isLocalSafari()}.
 */
public class ParallelConfigListener implements IAlterSuiteListener {

    private static final Logger log = LogManager.getLogger(ParallelConfigListener.class);
    private static final String SAFARI = "safari";
    private static final String LOCAL = "local";

    @Override
    public void alter(List<XmlSuite> suites) {
        boolean parallelEnabled = ConfigManager.getBoolean("parallel.execution", false);

        if (parallelEnabled && isLocalSafari()) {
            log.warn("Safari cannot run tests in parallel — safaridriver permits only one "
                    + "automation session per host. Forcing sequential execution; "
                    + "parallel.execution=true and thread.count are ignored for this run.");
            parallelEnabled = false;
        }

        for (XmlSuite suite : suites) {
            if (parallelEnabled) {
                String mode = ConfigManager.get("parallel.mode", "methods").toLowerCase();
                int threadCount = ConfigManager.getInt("thread.count", 2);

                XmlSuite.ParallelMode parallelMode = switch (mode) {
                    case "classes" -> XmlSuite.ParallelMode.CLASSES;
                    case "tests" -> XmlSuite.ParallelMode.TESTS;
                    default -> XmlSuite.ParallelMode.METHODS;
                };

                suite.setParallel(parallelMode);
                suite.setThreadCount(threadCount);

                log.info("Parallel execution ENABLED — mode: {}, threads: {}, suite: {}",
                        parallelMode, threadCount, suite.getName());
            } else {
                suite.setParallel(XmlSuite.ParallelMode.NONE);
                log.info("Parallel execution DISABLED (sequential) — suite: {}", suite.getName());
            }
        }
    }

    /**
     * True when the run targets Safari on this machine.
     *
     * <p>The single-session restriction belongs to the local {@code safaridriver} process, not to
     * Safari itself — a cloud grid hands out one VM per session, so remote Safari parallelises
     * normally and must not be throttled here.
     */
    private static boolean isLocalSafari() {
        return SAFARI.equalsIgnoreCase(ConfigManager.get("browser", "chrome").trim())
                && LOCAL.equalsIgnoreCase(ConfigManager.get("execution.mode", "local").trim());
    }
}
