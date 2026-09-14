package me.onethecrazy.util.render;

import com.aksulightning.fbxplayermodels.voice.VoiceAudioInput;
import com.aksulightning.fbxplayermodels.voice.VoiceShapePose;
import com.aksulightning.platform.PlatformServices;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.editor.VoiceShapeSettingsScreen;
import me.onethecrazy.util.objects.SkinnedModel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.util.Objects;

/** Local model settings are not applied to another player's model. */
public final class VoiceShapeClient {
    private static final boolean VOICE_CHAT_INSTALLED = FabricLoader.getInstance().isModLoaded("voicechat");
    private VoiceShapeClient() {}
    public static boolean voiceChatInstalled() { return VOICE_CHAT_INSTALLED; }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var options = FBXPlayerModelsClient.options();
            if (options == null || options.selectedSkin == null) { VoiceAudioInput.stop(); return; }
            VoiceAudioInput.configure(options.selectedSkin.voiceShapeSettings(), VOICE_CHAT_INSTALLED,
                    (client.player != null && options.areFbxPlayerModelsEnabled()) || client.gui.screen() instanceof VoiceShapeSettingsScreen);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoiceAudioInput.stop());
        PlatformServices.events().registerClientDisconnected(VoiceAudioInput::stop);
    }

    public static VoiceShapePose poseFor(String uuid, SkinnedModel model) {
        var options = FBXPlayerModelsClient.options();
        if (options == null || options.selectedSkin == null
                || !Objects.equals(uuid, PlatformServices.client().currentSessionUuid())) return VoiceShapePose.NONE;
        var lookup = SkinManager.skinLookup.get(uuid);
        if (lookup == null || !Objects.equals(lookup.hash, options.selectedSkin.hash)) return VoiceShapePose.NONE;
        var settings = options.selectedSkin.voiceShapeSettings();
        boolean found = switch (settings.target()) {
            case NONE -> false;
            case BONE -> model.bones.stream().anyMatch(bone -> Objects.equals(bone.name(), settings.name));
            case SHAPE_KEY -> model.shapeKeys.stream().anyMatch(key -> Objects.equals(key.id(), settings.shapeKeyId));
        };
        return found ? VoiceShapePose.from(settings, VoiceAudioInput.level()) : VoiceShapePose.NONE;
    }
}
