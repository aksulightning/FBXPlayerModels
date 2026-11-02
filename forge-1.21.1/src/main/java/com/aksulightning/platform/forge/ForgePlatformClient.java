package com.aksulightning.platform.forge;

import com.aksulightning.platform.PlatformClient;
import net.minecraft.client.Minecraft;

public final class ForgePlatformClient implements PlatformClient {
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
