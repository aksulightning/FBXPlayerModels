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
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.GameShuttingDownEvent;

import java.util.Objects;

/** Samples only the selected local model; no audio or animation packets are registered. */
public final class VoiceShapeClient {
    private VoiceShapeClient() {}

    public static boolean voiceChatInstalled() {
        return ModList.get().isLoaded("voicechat");
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> tick());
        MinecraftForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> VoiceAudioInput.stop());
        PlatformServices.events().registerClientDisconnected(VoiceAudioInput::stop);
    }

    private static void tick() {
        var client = Minecraft.getInstance();
        var options = FBXPlayerModelsClient.options();
        if (options.selectedSkin == null) {
            VoiceAudioInput.stop();
            return;
        }
        boolean editor = client.screen instanceof VoiceShapeSettingsScreen;
        boolean world = client.player != null && LanSession.isAllowed()
                && options.areFbxPlayerModelsEnabled() && SkinManager.getSelfSkin() != null;
        VoiceAudioInput.configure(options.selectedSkin.voiceShapeSettings(), voiceChatInstalled(), editor || world);
    }

    public static VoiceShapePose poseFor(SkinnedModel model) {
        var options = FBXPlayerModelsClient.options();
        var cache = SkinManager.getSelfSkin();
        if (cache == null || cache.skinnedModel != model || options.selectedSkin == null) return VoiceShapePose.NONE;
        VoiceShapeSettings settings = options.selectedSkin.voiceShapeSettings();
        boolean found = switch (settings.target()) {
            case NONE -> false;
            case BONE -> model.bones.stream().anyMatch(bone -> Objects.equals(bone.name(), settings.name));
            case SHAPE_KEY -> model.shapeKeys.stream().anyMatch(key -> Objects.equals(key.id(), settings.shapeKeyId));
        };
        return found ? VoiceShapePose.from(settings, VoiceAudioInput.level()) : VoiceShapePose.NONE;
    }
}
