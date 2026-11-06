package me.onethecrazy.util.render;

import com.aksulightning.fbxplayermodels.voice.VoiceAudioInput;
import com.aksulightning.fbxplayermodels.voice.VoiceShapePose;
import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;
import com.aksulightning.platform.PlatformServices;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.editor.VoiceShapeSettingsScreen;
import me.onethecrazy.util.network.LanSession;
import me.onethecrazy.util.objects.SkinnedModel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.util.Objects;

/** Resolves the selected local model's voice pose without sending audio or animation packets. */
public final class VoiceShapeClient {
    private static final boolean VOICE_CHAT_INSTALLED = FabricLoader.getInstance().isModLoaded("voicechat");

    private VoiceShapeClient() {}

    public static boolean voiceChatInstalled() { return VOICE_CHAT_INSTALLED; }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var options = FBXPlayerModelsClient.options();
            if (options.selectedSkin == null) {
                VoiceAudioInput.stop();
                return;
            }
            boolean editor = client.gui.screen() instanceof VoiceShapeSettingsScreen;
            boolean world = client.player != null && LanSession.isAllowed()
                    && options.areFbxPlayerModelsEnabled() && SkinManager.getSelfSkin() != null;
            VoiceAudioInput.configure(options.selectedSkin.voiceShapeSettings(), VOICE_CHAT_INSTALLED,
                    editor || world);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VoiceAudioInput.stop());
        PlatformServices.events().registerClientDisconnected(VoiceAudioInput::stop);
    }

    public static VoiceShapePose poseFor(SkinnedModel model) {
        var options = FBXPlayerModelsClient.options();
        var cache = SkinManager.getSelfSkin();
        if (cache == null || cache.skinnedModel != model || options.selectedSkin == null) {
            return VoiceShapePose.NONE;
        }
        VoiceShapeSettings settings = options.selectedSkin.voiceShapeSettings();
        boolean found = switch (settings.target()) {
            case NONE -> false;
            case BONE -> model.bones.stream().anyMatch(bone -> Objects.equals(bone.name(), settings.name));
            case SHAPE_KEY -> model.shapeKeys.stream().anyMatch(key -> Objects.equals(key.id(), settings.shapeKeyId));
        };
        return found ? VoiceShapePose.from(settings, VoiceAudioInput.level()) : VoiceShapePose.NONE;
    }
}
