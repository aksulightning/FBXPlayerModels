package me.onethecrazy.util.network;

import com.aksulightning.fbxplayermodels.network.LanAddressPolicy;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.mixin.client.ClientNetworkHandlerAccessor;
import net.minecraft.client.network.ClientNetworkHandler;

/** Beta is local rendering only: this class registers no packets or server code. */
public final class LanSession {
    private static ClientNetworkHandler observed;
    private static boolean noticeShown;

    private LanSession() {}

    public static boolean isAllowed() {
        var client = FBXPlayerModelsClient.minecraft();
        if (client == null || client.world == null) return false;
        if (!client.isWorldRemote()) return true;
        var handler = client.getNetworkHandler();
        return handler != null && LanAddressPolicy.isLanAddress(
                ((ClientNetworkHandlerAccessor) handler).fbx_player_models$getConnection().getAddress());
    }

    public static boolean isRenderingAllowed() {
        var client = FBXPlayerModelsClient.minecraft();
        return client != null && (client.world == null || isAllowed());
    }

    public static void tick() {
        var client = FBXPlayerModelsClient.minecraft();
        if (client == null) return;
        var handler = client.getNetworkHandler();
        if (handler != observed) {
            observed = handler;
            noticeShown = false;
        }
        if (handler != null && client.player != null && client.world != null
                && !isAllowed() && !noticeShown) {
            noticeShown = true;
            client.inGameHud.addChatMessage(LanAddressPolicy.UNSUPPORTED_MESSAGE);
        }
    }
}
