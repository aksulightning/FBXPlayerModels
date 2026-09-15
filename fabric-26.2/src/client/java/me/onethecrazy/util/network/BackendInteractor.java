package me.onethecrazy.util.network;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKeySyncState;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.SkinManager;
import me.onethecrazy.network.ModelPackets;
import me.onethecrazy.util.objects.LookupSkin;
import me.onethecrazy.util.objects.save.ClientSkin;
import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.render.VoiceShapeClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class BackendInteractor {
    private static final Map<String, List<CompletableFuture<Map<String, LookupSkin>>>> LOOKUP_WAITERS = new HashMap<>();
    private static final Map<String, List<Consumer<byte[]>>> MODEL_WAITERS = new HashMap<>();
    private static final Map<String, List<Consumer<byte[]>>> MOB_MODEL_WAITERS = new HashMap<>();
    private static final long SHAPE_SYNC_INTERVAL_NANOS = 200_000_000L;
    private static ShapeKeySyncState pendingShapeSettings;
    private static long lastShapeSync;
    private static boolean initialized;

    private BackendInteractor() {
    }

    public static void initializeClient() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientPlayNetworking.registerGlobalReceiver(ModelPackets.LookupResponsePayload.TYPE, (payload, context) ->
                context.client().execute(() -> receiveLookup(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ModelPackets.ModelDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> receiveModel(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ModelPackets.MobModelDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> receiveMobModel(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ModelPackets.UploadResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> showMessage(payload.message())));
        ClientPlayNetworking.registerGlobalReceiver(ModelPackets.PlayerShapeSettingsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> SkinManager.acceptServerShapeSettings(payload.uuid(), payload.settings())));
        ClientPlayNetworking.registerGlobalReceiver(ModelPackets.PlayerVoiceLevelPayload.TYPE, (payload, context) ->
                context.client().execute(() -> VoiceShapeClient.acceptRemoteLevel(payload.uuid(), payload.level())));
    }

    public static CompletableFuture<Map<String, LookupSkin>> getSkinIDs(List<String> uuids) {
        CompletableFuture<Map<String, LookupSkin>> future = new CompletableFuture<>();
        if (!canSend(ModelPackets.RequestLookupPayload.TYPE)) {
            future.complete(Map.of());
            return future;
        }

        synchronized (LOOKUP_WAITERS) {
            for (String uuid : uuids) {
                LOOKUP_WAITERS.computeIfAbsent(uuid, ignored -> new ArrayList<>()).add(future);
            }
        }
        ClientPlayNetworking.send(new ModelPackets.RequestLookupPayload(List.copyOf(uuids)));
        return future;
    }

    public static void getSkinData(LookupSkin skin, Consumer<byte[]> onArrive) {
        if (skin == null || skin.hash == null || skin.hash.isBlank() || skin.format == null) {
            onArrive.accept(new byte[0]);
            return;
        }
        if (!canSend(ModelPackets.RequestModelPayload.TYPE)) {
            onArrive.accept(new byte[0]);
            return;
        }

        String key = modelKey(skin.hash, skin.format.name());
        synchronized (MODEL_WAITERS) {
            MODEL_WAITERS.computeIfAbsent(key, ignored -> new ArrayList<>()).add(onArrive);
        }
        ClientPlayNetworking.send(new ModelPackets.RequestModelPayload(skin.hash, skin.format.name()));
    }

    public static void getMobModelData(String model, Consumer<byte[]> onArrive) {
        if (model == null || model.isBlank()) {
            onArrive.accept(new byte[0]);
            return;
        }
        if (!canSend(ModelPackets.RequestMobModelPayload.TYPE)) {
            onArrive.accept(new byte[0]);
            return;
        }

        synchronized (MOB_MODEL_WAITERS) {
            MOB_MODEL_WAITERS.computeIfAbsent(model, ignored -> new ArrayList<>()).add(onArrive);
        }
        ClientPlayNetworking.send(new ModelPackets.RequestMobModelPayload(model));
    }

    public static void setSkinData(String uuid, byte[] data3d, ParsingFormat format) {
        setSkinData(uuid, "model.fbx", data3d, format);
    }

    public static void setSkinData(String uuid, String fileName, byte[] data3d, ParsingFormat format) {
        setSkinData(uuid, fileName, data3d, format, false);
    }

    public static void setSkinData(String uuid, String fileName, byte[] data3d, ParsingFormat format, boolean quietWhenUnsupported) {
        if (!canSend(ModelPackets.UploadModelPayload.TYPE)) {
            if (!quietWhenUnsupported) {
                showMessage("Upload denied: this server does not support FBX Player Models uploads.");
            }
            return;
        }
        if (data3d != null && data3d.length > ModelPackets.MAX_MODEL_BYTES) {
            showMessage("Upload too large: model files must be under 2 MB.");
            return;
        }
        String formatName = format == null ? ParsingFormat.FBX.name() : format.name();
        ClientPlayNetworking.send(new ModelPackets.UploadModelPayload(fileName, formatName, data3d == null ? new byte[0] : data3d));
    }

    public static void syncShapeSettings(ClientSkin skin) {
        if (skin == null) return;
        pendingShapeSettings = ShapeKeySyncState.from(skin.hash, skin.defaultShapeKeyProfile(),
                skin.voiceShapeSettings());
        flushShapeSettings();
    }

    public static void flushShapeSettings() {
        long now = System.nanoTime();
        if (pendingShapeSettings == null || now - lastShapeSync < SHAPE_SYNC_INTERVAL_NANOS
                || !canSend(ModelPackets.UpdateShapeSettingsPayload.TYPE)) return;
        ClientPlayNetworking.send(new ModelPackets.UpdateShapeSettingsPayload(pendingShapeSettings));
        pendingShapeSettings = null;
        lastShapeSync = now;
    }

    public static void sendVoiceLevel(float level) {
        if (!canSend(ModelPackets.VoiceLevelPayload.TYPE)) return;
        ClientPlayNetworking.send(new ModelPackets.VoiceLevelPayload(
                Float.isFinite(level) ? Math.max(0f, Math.min(1f, level)) : 0f));
    }

    public static CompletableFuture<String> getBannerTextAsync() {
        return CompletableFuture.completedFuture("");
    }

    private static void receiveLookup(ModelPackets.LookupResponsePayload payload) {
        Map<String, LookupSkin> result = new HashMap<>();
        for (Map.Entry<String, ModelPackets.ModelLookup> entry : payload.models().entrySet()) {
            ParsingFormat format = parseFormat(entry.getValue().format());
            if (format != null) {
                result.put(entry.getKey(), new LookupSkin(entry.getValue().hash(), format));
            }
        }

        synchronized (LOOKUP_WAITERS) {
            for (Map.Entry<String, ModelPackets.ModelLookup> entry : payload.models().entrySet()) {
                String uuid = entry.getKey();
                List<CompletableFuture<Map<String, LookupSkin>>> waiters = LOOKUP_WAITERS.remove(uuid);
                if (waiters != null) {
                    for (CompletableFuture<Map<String, LookupSkin>> waiter : waiters) {
                        waiter.complete(result);
                    }
                } else {
                    SkinManager.acceptServerLookup(uuid, result.getOrDefault(uuid, new LookupSkin("", null)));
                }
            }
        }
    }

    private static void receiveModel(ModelPackets.ModelDataPayload payload) {
        if (payload.data().length == 0 && payload.message() != null && !payload.message().isBlank()) {
            FBXPlayerModelsMod.LOGGER.info(payload.message());
        }

        String key = modelKey(payload.hash(), payload.format());
        List<Consumer<byte[]>> waiters;
        synchronized (MODEL_WAITERS) {
            waiters = MODEL_WAITERS.remove(key);
        }
        if (waiters != null) {
            for (Consumer<byte[]> waiter : waiters) {
                waiter.accept(payload.data());
            }
        }
    }

    private static void receiveMobModel(ModelPackets.MobModelDataPayload payload) {
        if (payload.data().length == 0 && payload.message() != null && !payload.message().isBlank()) {
            FBXPlayerModelsMod.LOGGER.info(payload.message());
        }

        List<Consumer<byte[]>> waiters;
        synchronized (MOB_MODEL_WAITERS) {
            waiters = MOB_MODEL_WAITERS.remove(payload.model());
        }
        if (waiters != null) {
            for (Consumer<byte[]> waiter : waiters) {
                waiter.accept(payload.data());
            }
        }
    }

    private static boolean canSend(CustomPacketPayload.Type<?> type) {
        try {
            return Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(type);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String modelKey(String hash, String format) {
        return hash + "." + format.toUpperCase(Locale.ROOT);
    }

    private static ParsingFormat parseFormat(String format) {
        try {
            return ParsingFormat.valueOf(format.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void showMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }
}
