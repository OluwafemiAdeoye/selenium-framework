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
 */
public class ParallelConfigListener implements IAlterSuiteListener {

    private static final Logger log = LogManager.getLogger(ParallelConfigListener.class);

    @Override
    public void alter(List<XmlSuite> suites) {
        boolean parallelEnabled = ConfigManager.getBoolean("parallel.execution", false);

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
}
