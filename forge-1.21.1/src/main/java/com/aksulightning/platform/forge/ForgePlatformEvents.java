package com.aksulightning.platform.forge;

import com.aksulightning.platform.PlatformEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.util.function.Consumer;

public final class ForgePlatformEvents implements PlatformEvents {
    @Override
    public void registerClientJoined(Runnable callback) {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> callback.run());
    }

    @Override
    public void registerClientDisconnected(Runnable callback) {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> callback.run());
    }
    @Override
    public void registerClientStarted(Runnable callback) {
        // Wait for initial resource loading before creating dynamic model textures.
        MinecraftForge.EVENT_BUS.addListener(new Consumer<TickEvent.RenderTickEvent.Post>() {
            @Override
            public void accept(TickEvent.RenderTickEvent.Post event) {
                if (Minecraft.getInstance().getOverlay() == null) {
                    MinecraftForge.EVENT_BUS.unregister(this);
                    callback.run();
                }
            }
        });
    }
}
