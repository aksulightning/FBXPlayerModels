package me.onethecrazy;

import com.aksulightning.platform.PlatformServices;
import com.aksulightning.fbxplayermodels.voice.VoiceAudioInput;
import me.onethecrazy.util.ClientFileUtil;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.ModelNormalizer;
import me.onethecrazy.util.model.animation.LogicalRigAnimator;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.objects.save.ClientSkin;
import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.parsing.UniversalParser;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SkinManager {
    @Nullable private static CacheSkin selfSkin;
    private static String selfSkinHash = "";

    private SkinManager() {
    }

    public static void pickClientSkin() {
        ClientFileUtil.modelPickerDialog().thenAccept(file -> {
            if (file == null || file.isBlank()) {
                return;
            }

            PlatformServices.client().executeOnRenderThread(() -> selectSelfSkin(Path.of(file)));
        });
    }

    public static void selectSelfSkin(Path dataPath) {
        try {
            byte[] data3D = FileUtil.read3DDataFile(dataPath);
            ParsingFormat format = UniversalParser.getParsingFormat(dataPath);
            if (format != ParsingFormat.FBX) {
                FBXPlayerModelsMod.LOGGER.warn("Only FBX models are supported: {}", dataPath);
                return;
            }

            String hash = FileUtil.getSha256(data3D);
            FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, format), data3D);

            FBXPlayerModelsClient.options().selectedSkin = new ClientSkin(hash, dataPath.getFileName().toString(), format);
            FileUtil.writeSave(FBXPlayerModelsClient.options());
            VoiceAudioInput.stop();
            loadSelfSkin();
        } catch (Exception exception) {
            FBXPlayerModelsMod.LOGGER.info("Ran into an error while setting the local player model", exception);
        }
    }

    public static void resetSelfSkin() {
        FBXPlayerModelsClient.options().selectedSkin = new ClientSkin();
        selfSkin = null;
        selfSkinHash = "";
        VoiceAudioInput.stop();
        FileUtil.writeSave(FBXPlayerModelsClient.options());
    }

    public static void loadSelfSkin() {
        selfSkin = loadSelectedSkinPreview();
        var selected = FBXPlayerModelsClient.options().selectedSkin;
        selfSkinHash = selected == null ? "" : selected.hash;
    }

    public static @Nullable CacheSkin getSelfSkin() {
        var selected = FBXPlayerModelsClient.options().selectedSkin;
        return selected != null && Objects.equals(selfSkinHash, selected.hash) ? selfSkin : null;
    }

    public static @Nullable CacheSkin loadSelectedSkinPreview() {
        ClientSkin selectedSkin = FBXPlayerModelsClient.options().selectedSkin;
        if (selectedSkin == null || selectedSkin.hash == null || selectedSkin.hash.isBlank() || selectedSkin.format == null) {
            return null;
        }

        try {
            Path data3DPath = FileUtil.getSkinPath(selectedSkin.hash, selectedSkin.format);
            var skinnedModel = UniversalParser.parseSkinned(data3DPath, selectedSkin.format)
                    .map(ModelNormalizer::normalize)
                    .map(model -> withSavedAnimationSettings(model, selectedSkin));
            if (skinnedModel.isPresent()) {
                return new CacheSkin(skinnedModel.get(), selectedSkin.format);
            }
            List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(data3DPath, selectedSkin.format));
            return new CacheSkin(vertices, selectedSkin.format);
        } catch (Exception exception) {
            FBXPlayerModelsMod.LOGGER.info("Ran into an error while loading the local player model", exception);
            return null;
        }
    }

    public static void saveCurrentBinding() {
        FileUtil.writeSave(FBXPlayerModelsClient.options());
        loadSelfSkin();
    }

    public static void saveDefaultShapeKeyProfile() {
        var options = FBXPlayerModelsClient.options();
        CacheSkin cache = getSelfSkin();
        if (cache != null && cache.skinnedModel != null) {
            selfSkin = new CacheSkin(cache.skinnedModel.withShapeKeyProfile(
                    options.selectedSkin.defaultShapeKeyProfile()), cache.format);
        }
        FileUtil.writeSave(options);
    }

    public static void saveVoiceShapeSettings() {
        FileUtil.writeSave(FBXPlayerModelsClient.options());
    }

    private static SkinnedModel withSavedAnimationSettings(SkinnedModel model, ClientSkin selectedSkin) {
        selectedSkin.clipMappings().remove("Idle");

        Map<String, SkinnedModel.Animation> importedAnimations = new LinkedHashMap<>(model.animations);
        Map<String, SkinnedModel.Animation> animations = new LinkedHashMap<>(importedAnimations);
        LogicalRigAnimator.proceduralAnimations(model.bones, selectedSkin.binding()).forEach((name, generated) -> {
            SkinnedModel.Animation existing = animations.get(name);
            if (existing == null || existing.logicalRigDriven()) {
                animations.put(name, generated);
            }
        });

        for (Map.Entry<String, String> entry : selectedSkin.clipMappings().entrySet()) {
            SkinnedModel.Animation mapped = importedAnimations.get(entry.getValue());
            if (mapped != null) {
                animations.put(entry.getKey(), mapped);
            }
        }

        return model.withLogicalRigBinding(selectedSkin.binding())
                .withAnimations(animations)
                .withAnimationsEnabled(selectedSkin.animationsEnabled())
                .withShapeKeyProfile(selectedSkin.defaultShapeKeyProfile());
    }
}
