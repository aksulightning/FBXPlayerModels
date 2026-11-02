package me.onethecrazy.util.network;

import com.aksulightning.fbxplayermodels.network.LanAddressPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Client-only policy; no server installation or model-sharing protocol is needed. */
public final class LanSession {
    private static ClientPacketListener observed;
    private static boolean noticeShown;

    private LanSession() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> tick());
    }

    public static boolean isAllowed() {
        Minecraft client = Minecraft.getInstance();
        var handler = client.getConnection();
        if (handler == null || client.level == null) return false;
        var connection = handler.getConnection();
        if (connection.isMemoryConnection()) return client.getSingleplayerServer() != null;
        return LanAddressPolicy.isLanAddress(connection.getRemoteAddress());
    }

    public static boolean isRenderingAllowed() {
        return Minecraft.getInstance().level == null || isAllowed();
    }

    private static void tick() {
        Minecraft client = Minecraft.getInstance();
        var handler = client.getConnection();
        if (handler != observed) {
            observed = handler;
            noticeShown = false;
        }
        if (handler != null && client.player != null && client.level != null
                && !isAllowed() && !noticeShown) {
            noticeShown = true;
            client.player.displayClientMessage(Component.literal(LanAddressPolicy.UNSUPPORTED_MESSAGE), false);
        }
    }
}
