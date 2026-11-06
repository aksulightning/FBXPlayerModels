package com.aksulightning.fbxplayermodels.forge1122.client;

import com.aksulightning.fbxplayermodels.forge1122.FBXPlayerModelsMod;
import com.aksulightning.fbxplayermodels.forge1122.util.model.rig.LogicalRigBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = true;
    public boolean firstPerson = false;
    public boolean animations = true;
    public float scale = 1f;
    public float cameraX;
    public float cameraY;
    public float cameraZ;
    public String modelHash = "";
    public String modelName = "";
    public LogicalRigBinding binding = new LogicalRigBinding();
    public Map<String, String> clips = new LinkedHashMap<>();

    public static Path directory() {
        return Minecraft.getMinecraft().gameDir.toPath().resolve(".fbxplayermodels/forge-1.12.2");
    }

    public static Path models() {
        return directory().resolve("skins");
    }

    public static ClientConfig load() {
        Path file = directory().resolve("config.json");
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                ClientConfig config = GSON.fromJson(reader, ClientConfig.class);
                if (config != null) {
                    config.sanitize();
                    return config;
                }
            } catch (Exception exception) {
                FBXPlayerModelsMod.LOGGER.warn("Could not load Forge 1.12.2 settings", exception);
            }
        }
        return new ClientConfig();
    }

    private void sanitize() {
        if (modelHash == null || !modelHash.matches("[0-9a-f]{64}")) modelHash = "";
        if (modelName == null) modelName = "";
        if (binding == null || binding.bindings == null) binding = new LogicalRigBinding();
        if (clips == null) clips = new LinkedHashMap<>();
        scale = finiteClamp(scale, 0.1f, 4f, 1f);
        cameraX = finiteClamp(cameraX, -2f, 2f, 0f);
        cameraY = finiteClamp(cameraY, -2f, 2f, 0f);
        cameraZ = finiteClamp(cameraZ, -2f, 2f, 0f);
    }

    public static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public void save() {
        try {
            sanitize();
            Files.createDirectories(directory());
            Path temp = directory().resolve("config.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temp, directory().resolve("config.json"), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temp, directory().resolve("config.json"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            FBXPlayerModelsMod.LOGGER.error("Could not save Forge 1.12.2 settings", exception);
        }
    }
}
