package me.onethecrazy.util.network;

import com.aksulightning.fbxplayermodels.client.ViewEntityModelCache;
import com.aksulightning.fbxplayermodels.network.LanAddressPolicy;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.render.VoiceShapeClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/** Session approval belongs to one connection and is never persisted in configuration. */
public final class LanSession {
    private static ClientPacketListener approved;
    private static ClientPacketListener joined;
    private static int ticksWaiting;
    private static boolean noticeShown;
    private static boolean ready;

    private LanSession() {}

    public static void initialize() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joined = handler;
            ticksWaiting = 0;
            noticeShown = false;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (joined == null || joined != client.getConnection() || client.player == null) return;
            if (isAllowed()) {
                if (!ready) {
                    ready = true;
                    // Retry entities that loaded before the LAN approval reached this client.
                    SkinManager.clearWorldSkinState();
                    SkinManager.uploadSelectedSkin(true);
                    for (var player : client.level.players()) {
                        if (player != client.player) SkinManager.loadSkin(player.getStringUUID());
                    }
                }
                return;
            }
            if (noticeShown) return;
            if (++ticksWaiting >= 60) {
                noticeShown = true;
                client.player.sendSystemMessage(Component.literal(LanAddressPolicy.UNSUPPORTED_MESSAGE));
            }
        });
    }

    public static boolean isAllowed() {
        Minecraft client = Minecraft.getInstance();
        var handler = client.getConnection();
        if (handler == null || client.level == null) return false;
        var connection = handler.getConnection();
        if (connection.isMemoryConnection()) return client.getSingleplayerServer() != null;
        return approved == handler && LanAddressPolicy.isLanAddress(connection.getRemoteAddress());
    }

    public static String statusText() {
        return isAllowed() ? "Single-player / LAN active" : "Single-player and LAN only";
    }

    public static boolean isRenderingAllowed() {
        return Minecraft.getInstance().level == null || isAllowed();
    }

    public static void approve(ClientPacketListener handler, int protocol) {
        Minecraft client = Minecraft.getInstance();
        if (handler == null || handler != client.getConnection()
                || protocol != LanAddressPolicy.PROTOCOL_VERSION) return;
        var connection = handler.getConnection();
        if (!connection.isMemoryConnection() && !LanAddressPolicy.isLanAddress(connection.getRemoteAddress())) return;
        approved = handler;
    }

    public static void receive(ClientPacketListener handler, Runnable callback) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (handler != null && handler == client.getConnection() && isAllowed()) callback.run();
        });
    }

    private static void reset() {
        ready = false;
        approved = null;
        joined = null;
        ticksWaiting = 0;
        noticeShown = false;
        SkinManager.clearWorldSkinState();
        ViewEntityModelCache.clear();
        VoiceShapeClient.clearNetworkState();
        BackendInteractor.resetSession();
    }
}
