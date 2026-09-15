package me.onethecrazy.server;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKeySyncState;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.network.ModelPackets;
import me.onethecrazy.network.ModelPackets.LookupResponsePayload;
import me.onethecrazy.network.ModelPackets.ModelDataPayload;
import me.onethecrazy.network.ModelPackets.ModelLookup;
import me.onethecrazy.network.ModelPackets.MobModelDataPayload;
import me.onethecrazy.network.ModelPackets.UploadResultPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class ServerModelNetworking {
    private static final Map<MinecraftServer, ServerModelStore> STORES = new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> LAST_VOICE_PACKETS = new WeakHashMap<>();
    private static final long MIN_VOICE_PACKET_INTERVAL_NANOS = 25_000_000L;

    private ServerModelNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(ModelPackets.UploadModelPayload.ID, ModelPackets.UploadModelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.RequestLookupPayload.ID, ModelPackets.RequestLookupPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.RequestModelPayload.ID, ModelPackets.RequestModelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.RequestMobModelPayload.ID, ModelPackets.RequestMobModelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.UpdateShapeSettingsPayload.ID, ModelPackets.UpdateShapeSettingsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelPackets.VoiceLevelPayload.ID, ModelPackets.VoiceLevelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.LookupResponsePayload.ID, ModelPackets.LookupResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.ModelDataPayload.ID, ModelPackets.ModelDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.MobModelDataPayload.ID, ModelPackets.MobModelDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.UploadResultPayload.ID, ModelPackets.UploadResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.PlayerShapeSettingsPayload.ID, ModelPackets.PlayerShapeSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelPackets.PlayerVoiceLevelPayload.ID, ModelPackets.PlayerVoiceLevelPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.UploadModelPayload.ID, (payload, context) ->
                context.server().execute(() -> handleUpload(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestLookupPayload.ID, (payload, context) ->
                context.server().execute(() -> handleLookup(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestModelPayload.ID, (payload, context) ->
                context.server().execute(() -> handleModelRequest(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestMobModelPayload.ID, (payload, context) ->
                context.server().execute(() -> handleMobModelRequest(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.UpdateShapeSettingsPayload.ID, (payload, context) ->
                context.server().execute(() -> handleShapeSettings(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.VoiceLevelPayload.ID, (payload, context) ->
                context.server().execute(() -> handleVoiceLevel(context.player(), payload)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("fbxplayermodels")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("uploadperm")
                                .then(CommandManager.argument("playername", StringArgumentType.word())
                                        .then(CommandManager.literal("yes").executes(context -> setUploadPerm(context.getSource(), StringArgumentType.getString(context, "playername"), true)))
                                        .then(CommandManager.literal("no").executes(context -> setUploadPerm(context.getSource(), StringArgumentType.getString(context, "playername"), false)))))));
    }

    private static void handleUpload(ServerPlayerEntity player, ModelPackets.UploadModelPayload payload) {
        ServerModelStore store = store(player.getServer());
        ServerModelStore.UploadSaveResult result = store.saveUpload(player, payload.fileName(), payload.format(), payload.data());
        ServerPlayNetworking.send(player, new UploadResultPayload(result.success(), result.message()));

        if (result.success() && result.model() != null) {
            broadcastModelState(player.getServer(), result.model());
        }
    }

    private static void handleLookup(ServerPlayerEntity player, ModelPackets.RequestLookupPayload payload) {
        ServerModelStore store = store(player.getServer());
        Map<String, ModelLookup> response = new HashMap<>();
        for (String uuid : payload.uuids()) {
            response.put(uuid, store.lookup(uuid)
                    .map(model -> new ModelLookup(model.hash(), model.format()))
                    .orElseGet(() -> new ModelLookup("", "")));
        }
        ServerPlayNetworking.send(player, new LookupResponsePayload(response));
        for (String uuid : payload.uuids()) {
            ServerPlayNetworking.send(player, new ModelPackets.PlayerShapeSettingsPayload(uuid, store.shapeSettings(uuid)));
        }
    }

    private static void handleModelRequest(ServerPlayerEntity player, ModelPackets.RequestModelPayload payload) {
        try {
            byte[] data = store(player.getServer()).readModel(payload.hash(), payload.format()).orElse(null);
            if (data == null) {
                ServerPlayNetworking.send(player, new ModelDataPayload(payload.hash(), payload.format(), new byte[0], "Model not found."));
                return;
            }
            ServerPlayNetworking.send(player, new ModelDataPayload(payload.hash(), payload.format(), data, "Saved successfully."));
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while reading model", e);
            ServerPlayNetworking.send(player, new ModelDataPayload(payload.hash(), payload.format(), new byte[0], "Server-side failure: could not read model."));
        }
    }

    private static void handleMobModelRequest(ServerPlayerEntity player, ModelPackets.RequestMobModelPayload payload) {
        try {
            byte[] data = store(player.getServer()).readMobModel(payload.model()).orElse(null);
            if (data == null) {
                ServerPlayNetworking.send(player, new MobModelDataPayload(payload.model(), new byte[0], "Mob model not found."));
                return;
            }
            ServerPlayNetworking.send(player, new MobModelDataPayload(payload.model(), data, "Saved successfully."));
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while reading mob model", e);
            ServerPlayNetworking.send(player, new MobModelDataPayload(payload.model(), new byte[0], "Server-side failure: could not read mob model."));
        }
    }

    private static void handleShapeSettings(ServerPlayerEntity player, ModelPackets.UpdateShapeSettingsPayload payload) {
        store(player.getServer()).updateShapeSettings(player, payload.settings())
                .ifPresent(settings -> broadcastShapeSettings(player.getServer(), player.getUuidAsString(), settings));
    }

    private static void handleVoiceLevel(ServerPlayerEntity sender, ModelPackets.VoiceLevelPayload payload) {
        if (!Float.isFinite(payload.level()) || store(sender.getServer()).lookup(sender.getUuidAsString()).isEmpty()) return;
        long now = System.nanoTime();
        Map<UUID, Long> serverPackets = LAST_VOICE_PACKETS.computeIfAbsent(sender.getServer(), ignored -> new WeakHashMap<>());
        long previous = serverPackets.getOrDefault(sender.getUuid(), 0L);
        if (now - previous < MIN_VOICE_PACKET_INTERVAL_NANOS) return;
        serverPackets.put(sender.getUuid(), now);

        float level = Math.max(0f, Math.min(1f, payload.level()));
        ModelPackets.PlayerVoiceLevelPayload relayed = new ModelPackets.PlayerVoiceLevelPayload(sender.getUuidAsString(), level);
        for (ServerPlayerEntity player : PlayerLookup.tracking(sender)) {
            ServerPlayNetworking.send(player, relayed);
        }
    }

    private static int setUploadPerm(ServerCommandSource source, String playerName, boolean allowed) {
        try {
            store(source.getServer()).setUploadPermission(playerName, allowed);
            if (allowed) {
                source.sendFeedback(() -> Text.literal("Granted upload permission to " + playerName + "."), true);
            } else {
                source.sendFeedback(() -> Text.literal("Removed upload permission from " + playerName + "."), true);
            }
            return 1;
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while saving upload permission", e);
            source.sendError(Text.literal("Server-side failure: could not save upload permission."));
            return 0;
        }
    }

    private static void broadcastLookup(MinecraftServer server, ServerModelStore.StoredModel model) {
        LookupResponsePayload payload = new LookupResponsePayload(Map.of(model.uuid(), new ModelLookup(model.hash(), model.format())));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void broadcastModelState(MinecraftServer server, ServerModelStore.StoredModel model) {
        broadcastLookup(server, model);
        broadcastShapeSettings(server, model.uuid(), store(server).shapeSettings(model.uuid()));
    }

    private static void broadcastShapeSettings(MinecraftServer server, String uuid,
                                               ShapeKeySyncState settings) {
        ModelPackets.PlayerShapeSettingsPayload payload = new ModelPackets.PlayerShapeSettingsPayload(uuid, settings);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static synchronized ServerModelStore store(MinecraftServer server) {
        return STORES.computeIfAbsent(server, ServerModelStore::new);
    }
}
