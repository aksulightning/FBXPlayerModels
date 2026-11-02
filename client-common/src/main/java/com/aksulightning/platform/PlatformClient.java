package com.aksulightning.platform;

public interface PlatformClient {
    void executeOnRenderThread(Runnable task);

    String currentSessionUuid();
}
