package com.aksulightning.platform.fabric;

import com.aksulightning.platform.PlatformEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class FabricPlatformEvents implements PlatformEvents {
    @Override
    public void registerClientStarted(Runnable callback) {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> callback.run());
    }

    @Override
    public void registerClientJoined(Runnable callback) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(callback));
    }

    @Override
    public void registerClientDisconnected(Runnable callback) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
    }
}
