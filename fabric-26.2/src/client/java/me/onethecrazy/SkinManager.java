package me.onethecrazy;

import com.aksulightning.platform.PlatformServices;
import me.onethecrazy.util.*;
import me.onethecrazy.network.ModelPackets;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.LookupSkin;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.objects.save.ClientSkin;
import me.onethecrazy.util.model.animation.LogicalRigAnimator;
import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.parsing.UniversalParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SkinManager {
    public static Map<String, LookupSkin> skinLookup = Collections.synchronizedMap(new HashMap<>());
    public static Map<String, CacheSkin> skinCache = Collections.synchronizedMap(new HashMap<>());

    private static final AtomicInteger worldSkinGeneration = new AtomicInteger();

    public static void pickClientSkin(){

        // Open File picker dialogue
        ClientFileUtil.modelPickerDialog()
                // Execute when user completes File-Selection
                .thenAccept(f -> {
                    if(f == null || Objects.equals(f, ""))
                        return;

                    try{
                        long fileSize = Files.size(Path.of(f));

                        if(fileSize > ModelPackets.MAX_MODEL_BYTES){
                            ToastUtil.showFileTooLargeToast();
                            return;
                        }
                    }
                    catch(Exception ex) {
                        FBXPlayerModelsMod.LOGGER.info("Ran into error while getting file size in client skin picker: {0}", ex);
                        return;
                    }

                    // Execute on Render Thread
                    PlatformServices.client().executeOnRenderThread(() -> SkinManager.selectSelfSkin(Path.of(f)));
                });
    }

    public static void selectSelfSkin(Path dataPath){
        try{
            String sessionUuid = PlatformServices.client().currentSessionUuid();

            byte[] data3D = FileUtil.read3DDataFile(dataPath);
            String name = dataPath.getFileName().toString();
            String hash = FileUtil.getSha256(data3D);
            ParsingFormat format = UniversalParser.getParsingFormat(dataPath);

            if (format != ParsingFormat.FBX) {
                FBXPlayerModelsMod.LOGGER.warn("Only FBX models are supported now: {}", dataPath);
                return;
            }

            FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, format), data3D);

            ClientSkin skin = new ClientSkin(hash, name, format);
            FBXPlayerModelsClient.options().selectedSkin = skin;

            // Save the updated options:
            FileUtil.writeSave(FBXPlayerModelsClient.options());

            // Reload self skin
            loadSelfSkin();

            // Send update to server
            if (sessionUuid != null) {
                BackendInteractor.setSkinData(sessionUuid, name, data3D, format);
            }
        }
        catch(Exception ex){
            FBXPlayerModelsMod.LOGGER.info("Ran into error while setting self skin: {0}", ex);
        }
    }

    public static void resetSelfSkin(){
        String sessionUuid = PlatformServices.client().currentSessionUuid();

        FBXPlayerModelsClient.options().selectedSkin = new ClientSkin();

        // Reload self skin
        loadSelfSkin();

        // Save in options
        FileUtil.writeSave(FBXPlayerModelsClient.options());

        // Send update to server
        if (sessionUuid != null) {
            BackendInteractor.setSkinData(sessionUuid, new byte[0], ParsingFormat.FBX);
        }
    }

    public static void uploadSelectedSkin(boolean quietWhenUnsupported) {
        String sessionUuid = PlatformServices.client().currentSessionUuid();
        if (sessionUuid == null) {
            return;
        }

        ClientSkin selectedSkin = FBXPlayerModelsClient.options().selectedSkin;
        if (selectedSkin == null || selectedSkin.hash == null || selectedSkin.hash.isBlank() || selectedSkin.format == null) {
            if (!quietWhenUnsupported) {
                BackendInteractor.setSkinData(sessionUuid, new byte[0], ParsingFormat.FBX);
            }
            return;
        }

        try {
            Path dataPath = FileUtil.getSkinPath(selectedSkin.hash, selectedSkin.format);
            if (!Files.exists(dataPath) || !Files.isRegularFile(dataPath)) {
                FBXPlayerModelsMod.LOGGER.warn("Invalid file: selected model cache is missing: {}", dataPath);
                return;
            }
            long fileSize = Files.size(dataPath);
            if (fileSize > ModelPackets.MAX_MODEL_BYTES) {
                ToastUtil.showFileTooLargeToast();
                return;
            }

            byte[] data3D = FileUtil.read3DDataFile(dataPath);
            String fileName = selectedSkin.name == null || selectedSkin.name.isBlank() ? selectedSkin.hash + ".fbx" : selectedSkin.name;
            BackendInteractor.setSkinData(sessionUuid, fileName, data3D, selectedSkin.format, quietWhenUnsupported);
        } catch (Exception ex) {
            FBXPlayerModelsMod.LOGGER.info("Ran into error while uploading selected skin:", ex);
        }
    }

    public static void loadSelfSkin(){
        String sessionUuid = PlatformServices.client().currentSessionUuid();
        if (sessionUuid == null) {
            return;
        }

        String uuid = sessionUuid;

        // Set self skin to empty if we don't have a selected skin
        var selectedSkin = FBXPlayerModelsClient.options().selectedSkin;

        if(Objects.equals(selectedSkin.hash, "")){
            putLookupEntry(uuid, new LookupSkin("", null));
            putCacheEntry(uuid, (List<Vertex>) null, null);

            return;
        }

        // Load self skin
        try{
            CacheSkin cacheSkin = loadSelectedSkinPreview();
            if (cacheSkin == null) {
                return;
            }

            putLookupEntry(uuid, new LookupSkin(selectedSkin.hash, selectedSkin.format));
            skinCache.put(uuid, cacheSkin);
        }
        catch(Exception e){
            FBXPlayerModelsMod.LOGGER.info("Ran into error while loading self skin data3D content:", e);
        }
    }

    public static @Nullable CacheSkin loadSelectedSkinPreview() {
        ClientSkin selectedSkin = FBXPlayerModelsClient.options().selectedSkin;
        if (selectedSkin == null || Objects.equals(selectedSkin.hash, "") || selectedSkin.format == null) {
            return null;
        }

        try {
            Path data3DPath = FileUtil.getSkinPath(selectedSkin.hash, selectedSkin.format);
            var skinnedModel = UniversalParser.parseSkinned(data3DPath, selectedSkin.format)
                    .map(ModelNormalizer::normalize)
                    .map(model -> withSavedAnimationSettings(model, selectedSkin));
            List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(data3DPath, selectedSkin.format));

            return skinnedModel
                    .<CacheSkin>map(model -> new CacheSkin(model, selectedSkin.format))
                    .orElseGet(() -> new CacheSkin(vertices, selectedSkin.format));
        } catch (Exception e) {
            FBXPlayerModelsMod.LOGGER.info("Ran into error while loading selected skin preview:", e);
            return null;
        }
    }

    public static void loadSkin(String uuid){
        // /getSkin
        // -> check local skins
        //      If not in local skins -> /files
        //          If response empty -> set entry to null
        //          Else -> entry to deserialized data3D

        // Put uuid into cache so that we don't request for this uuid again in RenderMixin
        skinCache.put(uuid, null);
        skinLookup.put(uuid, new LookupSkin("", null));

        int generation = worldSkinGeneration.get();

        // Player was never encountered before
        BackendInteractor.getSkinIDs(List.of(uuid))
                .thenAccept(map -> {
                    if (generation != worldSkinGeneration.get()) {
                        return;
                    }

                    LookupSkin lookupSkin = map.get(uuid);
                    if (lookupSkin == null || lookupSkin.hash == null || lookupSkin.hash.isBlank() || lookupSkin.format == null) {
                        putLookupEntry(uuid, new LookupSkin("", null));
                        putCacheEntry(uuid, (List<Vertex>) null, null);
                        return;
                    }

                    putLookupEntry(uuid, lookupSkin);

                    // We don't have the skin loaded (or want it to be updated)
                    loadSkinIntoCache(uuid, generation);
                });
    }

    private static void loadSkinIntoCache(String uuid, int generation){
        if (generation != worldSkinGeneration.get()) {
            return;
        }

        LookupSkin lookupSkin = skinLookup.get(uuid);
        if (lookupSkin == null || lookupSkin.hash == null || lookupSkin.hash.isBlank() || lookupSkin.format == null) {
            putCacheEntry(uuid, (List<Vertex>) null, null);
            return;
        }

        // Load from I/O cache
        if(FileUtil.isSkinCached(lookupSkin.hash))
        {
            try {
                Path path = FileUtil.getSkinPath(lookupSkin.hash, lookupSkin.format);
                var skinnedModel = UniversalParser.parseSkinned(path, lookupSkin.format).map(ModelNormalizer::normalize);

                List<Vertex> vertices = ModelNormalizer.normalize(
                        UniversalParser.parse(
                            path
                        )
                );

                skinnedModel.ifPresentOrElse(
                        model -> {
                            if (generation == worldSkinGeneration.get()) {
                                putCacheEntry(uuid, model, lookupSkin.format);
                            }
                        },
                        () -> {
                            if (generation == worldSkinGeneration.get()) {
                                putCacheEntry(uuid, vertices, lookupSkin.format);
                            }
                        }
                );
            } catch (Exception e) {
                if (generation == worldSkinGeneration.get()) {
                    putCacheEntry(uuid, (List<Vertex>) null, null);
                }

                FBXPlayerModelsMod.LOGGER.error("Ran into error while loading skin from I/O Cache: {0}", e);
            }
        }
        // Request from Server
        else{
            BackendInteractor.getSkinData(lookupSkin, (data3D) -> {
                if (generation != worldSkinGeneration.get()) {
                    return;
                }

                if(data3D.length != 0){
                    var lookupResult = skinLookup.get(uuid);
                    if (lookupResult == null || lookupResult.hash == null || lookupResult.format == null) {
                        putCacheEntry(uuid, (List<Vertex>) null, null);
                        return;
                    }

                    String hash = lookupResult.hash;

                    // Try saving to local Cache
                    try {
                        FileUtil.createFileIfNotPresent(FileUtil.getSkinPath(hash, lookupResult.format), data3D);
                    } catch (IOException e) {
                        FBXPlayerModelsMod.LOGGER.error("Ran into error while saving skin to I/O cache: ", e);
                    }

                    Path path = FileUtil.getSkinPath(hash, lookupResult.format);
                    var skinnedModel = UniversalParser.parseSkinned(path, lookupResult.format).map(ModelNormalizer::normalize);
                    List<Vertex> vertices = ModelNormalizer.normalize(UniversalParser.parse(path));
                    skinnedModel.ifPresentOrElse(
                            model -> {
                                if (generation == worldSkinGeneration.get()) {
                                    putCacheEntry(uuid, model, lookupResult.format);
                                }
                            },
                            () -> {
                                if (generation == worldSkinGeneration.get()) {
                                    putCacheEntry(uuid, vertices, lookupResult.format);
                                }
                            }
                    );
                }
                else{
                    if (generation == worldSkinGeneration.get()) {
                        putCacheEntry(uuid, (List<Vertex>) null, null);
                    }
                }
            });
        }
    }

    public static void clearWorldSkinState() {
        worldSkinGeneration.incrementAndGet();

        String selfUuid = null;
        String sessionUuid = PlatformServices.client().currentSessionUuid();
        if (sessionUuid != null) {
            selfUuid = sessionUuid;
        }

        LookupSkin selfLookup = selfUuid == null ? null : skinLookup.get(selfUuid);
        CacheSkin selfCache = selfUuid == null ? null : skinCache.get(selfUuid);

        skinLookup.clear();
        skinCache.clear();

        if (selfUuid != null) {
            if (selfLookup != null) {
                skinLookup.put(selfUuid, selfLookup);
            }
            if (selfCache != null) {
                skinCache.put(selfUuid, selfCache);
            }
        }
    }

    public static void putCacheEntry(String uuid, @Nullable List<Vertex> vertices, ParsingFormat format){
        skinCache.put(uuid, new CacheSkin(vertices, format));
    }

    public static void putCacheEntry(String uuid, @Nullable SkinnedModel skinnedModel, ParsingFormat format){
        ClientSkin selected = FBXPlayerModelsClient.options().selectedSkin;
        LookupSkin lookup = skinLookup.get(uuid);
        if (skinnedModel != null && selected != null && lookup != null
                && Objects.equals(uuid, PlatformServices.client().currentSessionUuid())
                && Objects.equals(lookup.hash, selected.hash)) {
            skinnedModel = skinnedModel.withShapeKeyProfile(selected.defaultShapeKeyProfile());
        }
        skinCache.put(uuid, new CacheSkin(skinnedModel, format));
    }

    public static void putLookupEntry(String uuid, LookupSkin lookupSkin){
        skinLookup.put(uuid, lookupSkin);
    }

    public static void acceptServerLookup(String uuid, LookupSkin lookupSkin) {
        putLookupEntry(uuid, lookupSkin);
        if (lookupSkin == null || lookupSkin.hash == null || lookupSkin.hash.isBlank() || lookupSkin.format == null) {
            putCacheEntry(uuid, (List<Vertex>) null, null);
            return;
        }

        skinCache.remove(uuid);
        loadSkinIntoCache(uuid, worldSkinGeneration.get());
    }

    public static void saveCurrentBinding() {
        FileUtil.writeSave(FBXPlayerModelsClient.options());
        loadSelfSkin();
    }

    public static void saveDefaultShapeKeyProfile() {
        var options = FBXPlayerModelsClient.options();
        String uuid = PlatformServices.client().currentSessionUuid();
        CacheSkin cache = uuid == null ? null : skinCache.get(uuid);
        LookupSkin lookup = uuid == null ? null : skinLookup.get(uuid);
        if (cache != null && cache.skinnedModel != null && lookup != null
                && Objects.equals(lookup.hash, options.selectedSkin.hash)) {
            skinCache.put(uuid, new CacheSkin(cache.skinnedModel.withShapeKeyProfile(options.selectedSkin.defaultShapeKeyProfile()), cache.format));
        }
        FileUtil.writeSave(options);
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
