package me.onethecrazy.server;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKeySyncState;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.network.ModelPackets;
import me.onethecrazy.util.parsing.ParsingFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ServerModelStore {
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._ -]{0,127}");
    private static final Pattern SAFE_HASH = Pattern.compile("[a-f0-9]{64}");
    private static final String PERMISSIONS_FILE = "upload-permissions.txt";
    private static final String INDEX_FILE = "model-index.tsv";
    private static final String SHAPE_SETTINGS_FILE = "shape-settings.json";
    private static final long MAX_SHAPE_SETTINGS_FILE_BYTES = 2L * 1024L * 1024L;
    private static final Gson GSON = new Gson();
    private static final Type SHAPE_SETTINGS_TYPE = new TypeToken<Map<String, ShapeKeySyncState>>() { }.getType();

    private final MinecraftServer server;
    private final Path root;
    private final Path modelsDir;
    private final Path mobSkinsDir;
    private final Path permissionsPath;
    private final Path indexPath;
    private final Path shapeSettingsPath;
    private final Set<String> uploadPermissionNames = new HashSet<>();
    private final Map<String, StoredModel> modelsByPlayer = new HashMap<>();
    private final Map<String, ShapeKeySyncState> shapeSettingsByPlayer = new HashMap<>();

    public ServerModelStore(MinecraftServer server) {
        this.server = server;
        this.root = server.getWorldPath(LevelResource.ROOT).resolve(FBXPlayerModelsMod.MOD_ID).normalize();
        this.modelsDir = root.resolve("models").normalize();
        this.mobSkinsDir = root.resolve("mobskins").normalize();
        this.permissionsPath = root.resolve(PERMISSIONS_FILE).normalize();
        this.indexPath = root.resolve(INDEX_FILE).normalize();
        this.shapeSettingsPath = root.resolve(SHAPE_SETTINGS_FILE).normalize();
        load();
    }

    public synchronized boolean hasUploadPermission(ServerPlayer player) {
        return server.isSingleplayerOwner(player.nameAndId())
                || hasAdministratorRights(player)
                || uploadPermissionNames.contains(normalizeName(player.getGameProfile().name()));
    }

    public synchronized void setUploadPermission(String playerName, boolean allowed) throws IOException {
        String normalized = normalizeName(playerName);
        if (allowed) {
            uploadPermissionNames.add(normalized);
        } else {
            uploadPermissionNames.remove(normalized);
        }
        savePermissions();
    }

    public synchronized Optional<StoredModel> lookup(String uuid) {
        if (!isSafeUuid(uuid)) {
            return Optional.empty();
        }
        return Optional.ofNullable(modelsByPlayer.get(uuid));
    }

    public synchronized ShapeKeySyncState shapeSettings(String uuid) {
        StoredModel model = modelsByPlayer.get(uuid);
        if (model == null) return ShapeKeySyncState.empty("");
        ShapeKeySyncState settings = shapeSettingsByPlayer.get(uuid);
        if (settings == null || !model.hash().equals(settings.modelHash)) {
            return ShapeKeySyncState.empty(model.hash());
        }
        return settings.snapshot();
    }

    public synchronized Optional<ShapeKeySyncState> updateShapeSettings(ServerPlayer player,
                                                                         ShapeKeySyncState requested) {
        String uuid = player.getUUID().toString();
        StoredModel model = modelsByPlayer.get(uuid);
        ShapeKeySyncState settings = requested == null ? ShapeKeySyncState.empty("") : requested.snapshot();
        if (model == null || !model.hash().equals(settings.modelHash)) return Optional.empty();
        ShapeKeySyncState previous = shapeSettingsByPlayer.put(uuid, settings);
        try {
            saveShapeSettings();
            return Optional.of(settings);
        } catch (IOException | RuntimeException e) {
            if (previous == null) shapeSettingsByPlayer.remove(uuid);
            else shapeSettingsByPlayer.put(uuid, previous);
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while saving shape key settings", e);
            return Optional.empty();
        }
    }

    public synchronized Optional<byte[]> readModel(String hash, String format) throws IOException {
        Optional<ParsingFormat> parsedFormat = parseFormat(format);
        if (!isSafeHash(hash) || parsedFormat.isEmpty()) {
            return Optional.empty();
        }
        Path path = resolveModelPath(hash, parsedFormat.get());
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        long size = Files.size(path);
        if (size > ModelPackets.MAX_MODEL_BYTES) {
            FBXPlayerModelsMod.LOGGER.warn("Stored model exceeded size limit and will not be sent: {}", path);
            return Optional.empty();
        }
        return Optional.of(Files.readAllBytes(path));
    }

    public synchronized Optional<byte[]> readMobModel(String model) throws IOException {
        if (!isSafeFileName(model)) {
            return Optional.empty();
        }
        Path path = resolveMobModelPath(model);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        long size = Files.size(path);
        if (size > ModelPackets.MAX_MODEL_BYTES) {
            FBXPlayerModelsMod.LOGGER.warn("Server mob model exceeded size limit and will not be sent: {}", path);
            return Optional.empty();
        }
        return Optional.of(Files.readAllBytes(path));
    }

    public synchronized UploadSaveResult saveUpload(ServerPlayer player, String fileName, String formatName, byte[] data) {
        if (!hasUploadPermission(player)) {
            return UploadSaveResult.denied("Upload denied: you do not have permission to upload models.");
        }
        if (data == null) {
            return UploadSaveResult.denied("Invalid file: malformed upload packet.");
        }
        if (data.length == 0) {
            return clearUpload(player);
        }
        if (data.length > ModelPackets.MAX_MODEL_BYTES) {
            return UploadSaveResult.denied("Upload too large: model files must be under 2 MB.");
        }
        if (!isSafeFileName(fileName)) {
            return UploadSaveResult.denied("Invalid file: unsafe model file name.");
        }
        Optional<ParsingFormat> format = parseFormat(formatName);
        if (format.isEmpty() || format.get() != ParsingFormat.FBX) {
            return UploadSaveResult.denied("Invalid file: only FBX models are supported.");
        }

        try {
            Files.createDirectories(modelsDir);
            String hash = sha256(data);
            Path path = resolveModelPath(hash, format.get());
            Files.write(path, data);

            String uuid = player.getUUID().toString();
            StoredModel previous = modelsByPlayer.get(uuid);
            StoredModel stored = new StoredModel(uuid, hash, format.get().name(), fileName);
            modelsByPlayer.put(uuid, stored);
            if (previous == null || !previous.hash().equals(hash)) {
                shapeSettingsByPlayer.put(uuid, ShapeKeySyncState.empty(hash));
            }
            saveIndex();
            saveShapeSettings();
            return UploadSaveResult.saved(stored, "Upload allowed: saved successfully.");
        } catch (IOException | RuntimeException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while saving uploaded model", e);
            return UploadSaveResult.denied("Server-side failure: could not save model.");
        }
    }

    public synchronized UploadSaveResult clearUpload(ServerPlayer player) {
        try {
            String uuid = player.getUUID().toString();
            modelsByPlayer.remove(uuid);
            shapeSettingsByPlayer.remove(uuid);
            saveIndex();
            saveShapeSettings();
            return UploadSaveResult.saved(new StoredModel(uuid, "", "", ""), "Saved successfully: selected model cleared.");
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while clearing uploaded model", e);
            return UploadSaveResult.denied("Server-side failure: could not clear model.");
        }
    }

    private void load() {
        try {
            Files.createDirectories(modelsDir);
            Files.createDirectories(mobSkinsDir);
            loadPermissions();
            loadIndex();
            loadShapeSettings();
        } catch (IOException | RuntimeException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while loading model store", e);
        }
    }

    private void loadPermissions() throws IOException {
        uploadPermissionNames.clear();
        if (!Files.exists(permissionsPath)) {
            return;
        }
        for (String line : Files.readAllLines(permissionsPath, StandardCharsets.UTF_8)) {
            String normalized = normalizeName(line);
            if (!normalized.isBlank()) {
                uploadPermissionNames.add(normalized);
            }
        }
    }

    private void loadIndex() throws IOException {
        modelsByPlayer.clear();
        if (!Files.exists(indexPath)) {
            return;
        }
        for (String line : Files.readAllLines(indexPath, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\t", 4);
            if (parts.length == 4 && isSafeUuid(parts[0]) && isSafeHash(parts[1]) && parseFormat(parts[2]).isPresent() && isSafeFileName(parts[3])) {
                modelsByPlayer.put(parts[0], new StoredModel(parts[0], parts[1], parts[2], parts[3]));
            }
        }
    }

    private void savePermissions() throws IOException {
        Files.createDirectories(root);
        Files.write(permissionsPath, uploadPermissionNames.stream().sorted().toList(), StandardCharsets.UTF_8);
    }

    private void saveIndex() throws IOException {
        Files.createDirectories(root);
        StringBuilder builder = new StringBuilder();
        for (StoredModel model : modelsByPlayer.values()) {
            builder.append(model.uuid()).append('\t')
                    .append(model.hash()).append('\t')
                    .append(model.format()).append('\t')
                    .append(model.fileName()).append('\n');
        }
        Files.writeString(indexPath, builder.toString(), StandardCharsets.UTF_8);
    }

    private void loadShapeSettings() throws IOException {
        shapeSettingsByPlayer.clear();
        if (!Files.exists(shapeSettingsPath)) return;
        if (Files.size(shapeSettingsPath) > MAX_SHAPE_SETTINGS_FILE_BYTES) {
            FBXPlayerModelsMod.LOGGER.warn("Ignoring oversized server shape settings file: {}", shapeSettingsPath);
            return;
        }
        Map<String, ShapeKeySyncState> loaded = GSON.fromJson(
                Files.readString(shapeSettingsPath, StandardCharsets.UTF_8), SHAPE_SETTINGS_TYPE);
        if (loaded == null) return;
        for (Map.Entry<String, ShapeKeySyncState> entry : loaded.entrySet()) {
            StoredModel model = modelsByPlayer.get(entry.getKey());
            ShapeKeySyncState settings = entry.getValue() == null ? null : entry.getValue().snapshot();
            if (model != null && settings != null && model.hash().equals(settings.modelHash)) {
                shapeSettingsByPlayer.put(entry.getKey(), settings);
            }
        }
    }

    private void saveShapeSettings() throws IOException {
        Files.createDirectories(root);
        Map<String, ShapeKeySyncState> saved = new HashMap<>();
        for (Map.Entry<String, ShapeKeySyncState> entry : shapeSettingsByPlayer.entrySet()) {
            StoredModel model = modelsByPlayer.get(entry.getKey());
            ShapeKeySyncState settings = entry.getValue() == null ? null : entry.getValue().snapshot();
            if (model != null && settings != null && model.hash().equals(settings.modelHash)) {
                saved.put(entry.getKey(), settings);
            }
        }
        String json = GSON.toJson(saved, SHAPE_SETTINGS_TYPE);
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_SHAPE_SETTINGS_FILE_BYTES) {
            throw new IOException("Server shape settings exceed the persistence size limit");
        }
        Files.writeString(shapeSettingsPath, json, StandardCharsets.UTF_8);
    }

    private Path resolveModelPath(String hash, ParsingFormat format) {
        Path path = modelsDir.resolve(hash + "." + format.name().toLowerCase(Locale.ROOT)).normalize();
        if (!path.startsWith(modelsDir)) {
            throw new InvalidPathException(path.toString(), "Path escapes model directory");
        }
        return path;
    }

    private Path resolveMobModelPath(String model) {
        Path path = mobSkinsDir.resolve(model).normalize();
        if (!path.startsWith(mobSkinsDir)) {
            throw new InvalidPathException(path.toString(), "Path escapes mob model directory");
        }
        return path;
    }

    public static boolean isSafeFileName(String fileName) {
        if (fileName == null || !SAFE_FILE_NAME.matcher(fileName).matches()) {
            return false;
        }
        try {
            Path path = Path.of(fileName);
            return path.getNameCount() == 1 && path.getFileName().toString().equals(fileName) && fileName.toLowerCase(Locale.ROOT).endsWith(".fbx");
        } catch (InvalidPathException e) {
            return false;
        }
    }

    private static boolean isSafeUuid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isSafeHash(String hash) {
        return hash != null && SAFE_HASH.matcher(hash).matches();
    }

    private static Optional<ParsingFormat> parseFormat(String format) {
        if (format == null || format.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ParsingFormat.valueOf(format.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String normalizeName(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean hasAdministratorRights(ServerPlayer player) {
        return player.permissions() instanceof LevelBasedPermissionSet permissionSet
                && permissionSet.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record StoredModel(String uuid, String hash, String format, String fileName) {
    }

    public record UploadSaveResult(boolean success, StoredModel model, String message) {
        static UploadSaveResult saved(StoredModel model, String message) {
            return new UploadSaveResult(true, model, message);
        }

        static UploadSaveResult denied(String message) {
            return new UploadSaveResult(false, null, message);
        }
    }
}
