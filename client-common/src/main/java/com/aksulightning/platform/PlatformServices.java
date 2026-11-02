package com.aksulightning.platform;

public final class PlatformServices {
    private static PlatformLogger logger;
    private static PlatformConfig config;
    private static PlatformClient client;
    private static PlatformEvents events;

    private PlatformServices() {
    }

    public static void initialize(PlatformLogger logger, PlatformConfig config, PlatformClient client, PlatformEvents events) {
        PlatformServices.logger = logger;
        PlatformServices.config = config;
        PlatformServices.client = client;
        PlatformServices.events = events;
    }

    public static PlatformLogger logger() {
        if (logger == null) {
            throw new IllegalStateException("Platform logger has not been initialized");
        }
        return logger;
    }

    public static PlatformConfig config() {
        if (config == null) {
            throw new IllegalStateException("Platform config has not been initialized");
        }
        return config;
    }

    public static PlatformClient client() {
        if (client == null) {
            throw new IllegalStateException("Platform client has not been initialized");
        }
        return client;
    }

    public static PlatformEvents events() {
        if (events == null) {
            throw new IllegalStateException("Platform events have not been initialized");
        }
        return events;
    }
}
