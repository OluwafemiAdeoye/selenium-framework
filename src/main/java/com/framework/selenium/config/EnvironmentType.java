package com.framework.selenium.config;

public enum EnvironmentType {
    LOCAL, STAGING, PRODUCTION;

    public static EnvironmentType fromString(String env) {
        try {
            return EnvironmentType.valueOf(env.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return LOCAL;
        }
    }
}
