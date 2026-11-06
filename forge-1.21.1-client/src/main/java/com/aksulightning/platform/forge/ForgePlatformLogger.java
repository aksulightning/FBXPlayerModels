package com.aksulightning.platform.forge;

import com.aksulightning.platform.PlatformLogger;
import org.slf4j.Logger;

public final class ForgePlatformLogger implements PlatformLogger {
    private final Logger delegate;

    public ForgePlatformLogger(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String message, Object... args) {
        delegate.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        delegate.warn(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        delegate.error(message, args);
    }
}
