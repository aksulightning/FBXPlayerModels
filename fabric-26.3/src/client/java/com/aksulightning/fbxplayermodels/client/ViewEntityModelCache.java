package com.aksulightning.fbxplayermodels.client;

import com.aksulightning.fbxplayermodels.ViewEntityModelPath;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.ModelNormalizer;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.network.LanSession;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.parsing.UniversalParser;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ViewEntityModelCache {
    private static final Map<String, CacheSkin> CACHE = new ConcurrentHashMap<>();
    private static long generation;
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();

    private ViewEntityModelCache() {
    }

    public static @Nullable CacheSkin get(String rawModel) {
        if (!LanSession.isAllowed()) return null;
        String model = ViewEntityModelPath.safeModelOrEmpty(rawModel);
        if (model.isBlank()) {
            return null;
        }
        CacheSkin cached = CACHE.get(model);
        if (cached != null) {
            return cached;
        }
        if (REQUESTED.add(model)) {
            long requestedGeneration = generation;
            BackendInteractor.getMobModelData(model, data -> {
                if (requestedGeneration == generation && LanSession.isAllowed()) receive(model, data);
            });
        }
        return null;
    }

    public static void clear() {
        generation++;
        CACHE.clear();
        REQUESTED.clear();
    }

    private static void receive(String model, byte[] data) {
        if (data == null || data.length == 0) {
            CACHE.put(model, CacheSkin.empty());
            return;
        }

        try {
            Path path = FileUtil.getMobModelCachePath(model).normalize();
            if (!path.startsWith(FileUtil.getMobModelCachePath())) {
                CACHE.put(model, CacheSkin.empty());
                return;
            }
            Files.createDirectories(FileUtil.getMobModelCachePath());
            Files.write(path, data);
            CACHE.put(model, load(path));
        } catch (Exception e) {
            FBXPlayerModelsMod.LOGGER.warn("Failed to cache view entity model {}", model, e);
            CACHE.put(model, CacheSkin.empty());
        }
    }

    private static CacheSkin load(Path path) {
        try {
            var skinnedModel = UniversalParser.parseSkinned(path, ParsingFormat.FBX).map(ModelNormalizer::normalize);
            if (skinnedModel.isPresent()) {
                return new CacheSkin(skinnedModel.get(), ParsingFormat.FBX);
            }

            return new CacheSkin(ModelNormalizer.normalize(UniversalParser.parse(path, ParsingFormat.FBX)), ParsingFormat.FBX);
        } catch (Exception e) {
            FBXPlayerModelsMod.LOGGER.warn("Failed to load view entity model {}", path.getFileName(), e);
            return CacheSkin.empty();
        }
    }
}
