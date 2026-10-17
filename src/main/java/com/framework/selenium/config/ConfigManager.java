package com.framework.selenium.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration properties with three-tier resolution:
 *   1. System properties (-Dkey=value) — highest priority
 *   2. Environment-specific file (config-{env}.properties)
 *   3. Base config.properties — lowest priority
 */
public final class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties("config/config.properties");
        String env = System.getProperty("env", "").toLowerCase();
        if (!env.isEmpty()) {
            loadProperties("config/config-" + env + ".properties");
        }
    }

    private ConfigManager() {
    }

    private static void loadProperties(String path) {
        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                Properties overlay = new Properties();
                overlay.load(is);
                PROPERTIES.putAll(overlay);
                log.info("Loaded configuration from: {}", path);
            } else {
                log.warn("Configuration file not found: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to load configuration: {}", path, e);
        }
    }

    public static String get(String key) {
        String sysValue = System.getProperty(key);
        return sysValue != null ? sysValue : PROPERTIES.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        return value != null ? Integer.parseInt(value.trim()) : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }
}
