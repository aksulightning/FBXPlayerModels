package com.aksulightning.platform.fabric;

import com.aksulightning.platform.PlatformClient;
import net.minecraft.client.Minecraft;

public class FabricPlatformClient implements PlatformClient {
    @Override
    public void executeOnRenderThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    @Override
    public String currentSessionUuid() {
        var uuid = Minecraft.getInstance().getUser().getProfileId();
        return uuid == null ? null : uuid.toString();
    }
}
