package com.framework.selenium.listeners;

import com.framework.selenium.config.ConfigManager;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Retries failed tests up to a configurable maximum count.
 * Uses {@link AtomicInteger} so the counter is safe when parallel execution
 * is enabled via {@code -Dparallel=methods}.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private final AtomicInteger retryCount = new AtomicInteger(0);

    @Override
    public boolean retry(ITestResult result) {
        int maxRetries = ConfigManager.getInt("max.retry.count", 1);
        if (retryCount.getAndIncrement() < maxRetries) {
            return true;
        }
        return false;
    }
}
