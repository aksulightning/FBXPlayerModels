package com.aksulightning.platform.fabric;

import com.aksulightning.platform.PlatformClient;
import net.minecraft.client.MinecraftClient;

public class FabricPlatformClient implements PlatformClient {
    @Override
    public void executeOnRenderThread(Runnable task) {
        MinecraftClient.getInstance().send(task);
    }

    @Override
    public String currentSessionUuid() {
        var uuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
        return uuid == null ? null : uuid.toString();
    }
}
