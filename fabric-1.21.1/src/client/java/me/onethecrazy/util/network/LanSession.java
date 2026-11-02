package me.onethecrazy.util.network;

import com.aksulightning.fbxplayermodels.client.ViewEntityModelCache;
import com.aksulightning.fbxplayermodels.network.LanAddressPolicy;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.render.VoiceShapeClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

/** Session approval belongs to one connection and is never persisted in configuration. */
public final class LanSession {
    private static ClientPlayNetworkHandler approved;
    private static ClientPlayNetworkHandler joined;
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
            if (joined == null || joined != client.getNetworkHandler() || client.player == null) return;
            if (isAllowed()) {
                if (!ready) {
                    ready = true;
                    // Retry entities that loaded before the LAN approval reached this client.
                    SkinManager.clearWorldSkinState();
                    SkinManager.uploadSelectedSkin(true);
                    for (var player : client.world.getPlayers()) {
                        if (player != client.player) SkinManager.loadSkin(player.getUuidAsString());
                    }
                }
                return;
            }
            if (noticeShown) return;
            if (++ticksWaiting >= 60) {
                noticeShown = true;
                client.player.sendMessage(Text.literal(LanAddressPolicy.UNSUPPORTED_MESSAGE), false);
            }
        });
    }

    public static boolean isAllowed() {
        MinecraftClient client = MinecraftClient.getInstance();
        var handler = client.getNetworkHandler();
        if (handler == null || client.world == null) return false;
        var connection = handler.getConnection();
        if (connection.isLocal()) return client.getServer() != null;
        return approved == handler && LanAddressPolicy.isLanAddress(connection.getAddress());
    }

    public static String statusText() {
        return isAllowed() ? "Single-player / LAN active" : "Single-player and LAN only";
    }

    public static boolean isRenderingAllowed() {
        return MinecraftClient.getInstance().world == null || isAllowed();
    }

    public static void approve(ClientPlayNetworkHandler handler, int protocol) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (handler == null || handler != client.getNetworkHandler()
                || protocol != LanAddressPolicy.PROTOCOL_VERSION) return;
        var connection = handler.getConnection();
        if (!connection.isLocal() && !LanAddressPolicy.isLanAddress(connection.getAddress())) return;
        approved = handler;
    }

    public static void receive(ClientPlayNetworkHandler handler, Runnable callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (handler != null && handler == client.getNetworkHandler() && isAllowed()) callback.run();
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
