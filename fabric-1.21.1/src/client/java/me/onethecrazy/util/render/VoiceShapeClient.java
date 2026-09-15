package me.onethecrazy.util.render;

import com.aksulightning.fbxplayermodels.voice.VoiceAudioInput;
import com.aksulightning.fbxplayermodels.voice.VoiceShapePose;
import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;
import com.aksulightning.platform.PlatformServices;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.editor.VoiceShapeSettingsScreen;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.SkinnedModel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Samples local voice input and resolves server-relayed voice poses for every rendered player. */
public final class VoiceShapeClient {
    private static final boolean VOICE_CHAT_INSTALLED = FabricLoader.getInstance().isModLoaded("voicechat");
    private static final long VOICE_HEARTBEAT_NANOS = 250_000_000L;
    private static final long REMOTE_LEVEL_HOLD_NANOS = 250_000_000L;
    private static final long REMOTE_LEVEL_EXPIRE_NANOS = 500_000_000L;
    private static final Map<String, RemoteLevel> REMOTE_LEVELS = new ConcurrentHashMap<>();
    private static float lastSentLevel = Float.NaN;
    private static long lastVoiceSend;
    private VoiceShapeClient() {}
    public static boolean voiceChatInstalled() { return VOICE_CHAT_INSTALLED; }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var options = FBXPlayerModelsClient.options();
            if (options == null || options.selectedSkin == null) {
                VoiceAudioInput.stop();
                sendLocalLevel(0f, false);
                return;
            }
            BackendInteractor.flushShapeSettings();
            VoiceAudioInput.configure(options.selectedSkin.voiceShapeSettings(), VOICE_CHAT_INSTALLED,
                    (client.player != null && options.areFbxPlayerModelsEnabled()) || client.currentScreen instanceof VoiceShapeSettingsScreen);
            boolean transmit = client.player != null && options.areFbxPlayerModelsEnabled()
                    && options.selectedSkin.voiceShapeSettings().enabled();
            sendLocalLevel(transmit ? VoiceAudioInput.level() : 0f, transmit);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoiceAudioInput.stop());
        PlatformServices.events().registerClientDisconnected(VoiceAudioInput::stop);
        PlatformServices.events().registerClientDisconnected(VoiceShapeClient::clearNetworkState);
    }

    public static VoiceShapePose poseFor(String uuid, SkinnedModel model) {
        var options = FBXPlayerModelsClient.options();
        var lookup = SkinManager.skinLookup.get(uuid);
        if (lookup == null) return VoiceShapePose.NONE;

        VoiceShapeSettings settings;
        float amount;
        if (options != null && options.selectedSkin != null
                && Objects.equals(uuid, PlatformServices.client().currentSessionUuid())) {
            if (!Objects.equals(lookup.hash, options.selectedSkin.hash)) return VoiceShapePose.NONE;
            settings = options.selectedSkin.voiceShapeSettings();
            amount = VoiceAudioInput.level();
        } else {
            var synced = SkinManager.shapeSettings.get(uuid);
            if (synced == null || !Objects.equals(lookup.hash, synced.modelHash)) return VoiceShapePose.NONE;
            settings = synced.voiceShapeSettings;
            amount = remoteLevel(uuid);
        }
        boolean found = switch (settings.target()) {
            case NONE -> false;
            case BONE -> model.bones.stream().anyMatch(bone -> Objects.equals(bone.name(), settings.name));
            case SHAPE_KEY -> model.shapeKeys.stream().anyMatch(key -> Objects.equals(key.id(), settings.shapeKeyId));
        };
        return found ? VoiceShapePose.from(settings, amount) : VoiceShapePose.NONE;
    }

    public static void acceptRemoteLevel(String uuid, float level) {
        if (uuid == null || Objects.equals(uuid, PlatformServices.client().currentSessionUuid()) || !Float.isFinite(level)) return;
        REMOTE_LEVELS.put(uuid, new RemoteLevel(Math.max(0f, Math.min(1f, level)), System.nanoTime()));
    }

    private static void sendLocalLevel(float level, boolean active) {
        long now = System.nanoTime();
        float safe = Float.isFinite(level) ? Math.max(0f, Math.min(1f, level)) : 0f;
        if (!active) {
            if (!Float.isNaN(lastSentLevel) && lastSentLevel != 0f) BackendInteractor.sendVoiceLevel(0f);
            lastSentLevel = 0f;
            lastVoiceSend = now;
            return;
        }
        if (Float.isNaN(lastSentLevel) || Math.abs(safe - lastSentLevel) >= 0.01f
                || now - lastVoiceSend >= VOICE_HEARTBEAT_NANOS) {
            BackendInteractor.sendVoiceLevel(safe);
            lastSentLevel = safe;
            lastVoiceSend = now;
        }
    }

    private static float remoteLevel(String uuid) {
        RemoteLevel remote = REMOTE_LEVELS.get(uuid);
        if (remote == null) return 0f;
        long age = System.nanoTime() - remote.receivedAt();
        if (age >= REMOTE_LEVEL_EXPIRE_NANOS) {
            REMOTE_LEVELS.remove(uuid, remote);
            return 0f;
        }
        if (age <= REMOTE_LEVEL_HOLD_NANOS) return remote.level();
        return remote.level() * (REMOTE_LEVEL_EXPIRE_NANOS - age)
                / (float) (REMOTE_LEVEL_EXPIRE_NANOS - REMOTE_LEVEL_HOLD_NANOS);
    }

    private static void clearNetworkState() {
        REMOTE_LEVELS.clear();
        lastSentLevel = Float.NaN;
        lastVoiceSend = 0L;
    }

    private record RemoteLevel(float level, long receivedAt) {}
}
