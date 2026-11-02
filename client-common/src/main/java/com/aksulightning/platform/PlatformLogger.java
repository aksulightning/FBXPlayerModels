package com.aksulightning.platform;

public interface PlatformLogger {
    void info(String message, Object... args);

    void warn(String message, Object... args);

    void error(String message, Object... args);
}
