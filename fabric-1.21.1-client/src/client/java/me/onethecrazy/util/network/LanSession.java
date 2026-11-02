package me.onethecrazy.util.network;

import com.aksulightning.fbxplayermodels.network.LanAddressPolicy;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

/** Client-local support policy; no payloads, receivers, or server installation. */
public final class LanSession {
    private static ClientPlayNetworkHandler observed;
    private static boolean noticeShown;

    private LanSession() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static boolean isAllowed() {
        MinecraftClient client = MinecraftClient.getInstance();
        var handler = client.getNetworkHandler();
        if (handler == null || client.world == null) return false;
        var connection = handler.getConnection();
        if (connection.isLocal()) return client.getServer() != null;
        return LanAddressPolicy.isLanAddress(connection.getAddress());
    }

    public static boolean isRenderingAllowed() {
        return MinecraftClient.getInstance().world == null || isAllowed();
    }

    private static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        var handler = client.getNetworkHandler();
        if (handler != observed) {
            observed = handler;
            noticeShown = false;
        }
        if (handler != null && client.player != null && client.world != null
                && !isAllowed() && !noticeShown) {
            noticeShown = true;
            client.player.sendMessage(Text.literal(LanAddressPolicy.UNSUPPORTED_MESSAGE), false);
        }
    }
}
