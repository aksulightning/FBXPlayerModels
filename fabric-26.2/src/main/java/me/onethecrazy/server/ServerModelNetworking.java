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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

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
        PayloadTypeRegistry.serverboundPlay().registerLarge(ModelPackets.UploadModelPayload.TYPE, ModelPackets.UploadModelPayload.CODEC, ModelPackets.MAX_MODEL_BYTES + 512);
        PayloadTypeRegistry.serverboundPlay().register(ModelPackets.RequestLookupPayload.TYPE, ModelPackets.RequestLookupPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModelPackets.RequestModelPayload.TYPE, ModelPackets.RequestModelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModelPackets.RequestMobModelPayload.TYPE, ModelPackets.RequestMobModelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().registerLarge(ModelPackets.UpdateShapeSettingsPayload.TYPE,
                ModelPackets.UpdateShapeSettingsPayload.CODEC, ModelPackets.MAX_SHAPE_SETTINGS_BYTES);
        PayloadTypeRegistry.serverboundPlay().register(ModelPackets.VoiceLevelPayload.TYPE, ModelPackets.VoiceLevelPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ModelPackets.LookupResponsePayload.TYPE, ModelPackets.LookupResponsePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().registerLarge(ModelPackets.ModelDataPayload.TYPE, ModelPackets.ModelDataPayload.CODEC, ModelPackets.MAX_MODEL_BYTES + 512);
        PayloadTypeRegistry.clientboundPlay().registerLarge(ModelPackets.MobModelDataPayload.TYPE, ModelPackets.MobModelDataPayload.CODEC, ModelPackets.MAX_MODEL_BYTES + 512);
        PayloadTypeRegistry.clientboundPlay().register(ModelPackets.UploadResultPayload.TYPE, ModelPackets.UploadResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().registerLarge(ModelPackets.PlayerShapeSettingsPayload.TYPE,
                ModelPackets.PlayerShapeSettingsPayload.CODEC, ModelPackets.MAX_SHAPE_SETTINGS_BYTES);
        PayloadTypeRegistry.clientboundPlay().register(ModelPackets.PlayerVoiceLevelPayload.TYPE, ModelPackets.PlayerVoiceLevelPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.UploadModelPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleUpload(context.server(), context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestLookupPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleLookup(context.server(), context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestModelPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleModelRequest(context.server(), context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.RequestMobModelPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleMobModelRequest(context.server(), context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.UpdateShapeSettingsPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleShapeSettings(context.server(), context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ModelPackets.VoiceLevelPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleVoiceLevel(context.server(), context.player(), payload)));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("fbxplayermodels")
                        .requires(ServerModelNetworking::hasAdministratorRights)
                        .then(Commands.literal("uploadperm")
                                .then(Commands.argument("playername", StringArgumentType.word())
                                        .then(Commands.literal("yes").executes(context -> setUploadPerm(context.getSource(), StringArgumentType.getString(context, "playername"), true)))
                                        .then(Commands.literal("no").executes(context -> setUploadPerm(context.getSource(), StringArgumentType.getString(context, "playername"), false)))))));
    }

    private static void handleUpload(MinecraftServer server, ServerPlayer player, ModelPackets.UploadModelPayload payload) {
        ServerModelStore store = store(server);
        ServerModelStore.UploadSaveResult result = store.saveUpload(player, payload.fileName(), payload.format(), payload.data());
        ServerPlayNetworking.send(player, new UploadResultPayload(result.success(), result.message()));

        if (result.success() && result.model() != null) {
            broadcastModelState(server, result.model());
        }
    }

    private static void handleLookup(MinecraftServer server, ServerPlayer player, ModelPackets.RequestLookupPayload payload) {
        ServerModelStore store = store(server);
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

    private static void handleModelRequest(MinecraftServer server, ServerPlayer player, ModelPackets.RequestModelPayload payload) {
        try {
            byte[] data = store(server).readModel(payload.hash(), payload.format()).orElse(null);
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

    private static void handleMobModelRequest(MinecraftServer server, ServerPlayer player, ModelPackets.RequestMobModelPayload payload) {
        try {
            byte[] data = store(server).readMobModel(payload.model()).orElse(null);
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

    private static void handleShapeSettings(MinecraftServer server, ServerPlayer player,
                                            ModelPackets.UpdateShapeSettingsPayload payload) {
        store(server).updateShapeSettings(player, payload.settings())
                .ifPresent(settings -> broadcastShapeSettings(server, player.getUUID().toString(), settings));
    }

    private static void handleVoiceLevel(MinecraftServer server, ServerPlayer sender,
                                         ModelPackets.VoiceLevelPayload payload) {
        if (!Float.isFinite(payload.level()) || store(server).lookup(sender.getUUID().toString()).isEmpty()) return;
        long now = System.nanoTime();
        Map<UUID, Long> serverPackets = LAST_VOICE_PACKETS.computeIfAbsent(server, ignored -> new WeakHashMap<>());
        long previous = serverPackets.getOrDefault(sender.getUUID(), 0L);
        if (now - previous < MIN_VOICE_PACKET_INTERVAL_NANOS) return;
        serverPackets.put(sender.getUUID(), now);

        float level = Math.max(0f, Math.min(1f, payload.level()));
        ModelPackets.PlayerVoiceLevelPayload relayed = new ModelPackets.PlayerVoiceLevelPayload(sender.getUUID().toString(), level);
        for (ServerPlayer player : PlayerLookup.tracking(sender)) {
            ServerPlayNetworking.send(player, relayed);
        }
    }

    private static int setUploadPerm(CommandSourceStack source, String playerName, boolean allowed) {
        try {
            store(source.getServer()).setUploadPermission(playerName, allowed);
            if (allowed) {
                source.sendSuccess(() -> Component.literal("Granted upload permission to " + playerName + "."), true);
            } else {
                source.sendSuccess(() -> Component.literal("Removed upload permission from " + playerName + "."), true);
            }
            return 1;
        } catch (IOException e) {
            FBXPlayerModelsMod.LOGGER.error("Server-side failure while saving upload permission", e);
            source.sendFailure(Component.literal("Server-side failure: could not save upload permission."));
            return 0;
        }
    }

    private static void broadcastLookup(MinecraftServer server, ServerModelStore.StoredModel model) {
        LookupResponsePayload payload = new LookupResponsePayload(Map.of(model.uuid(), new ModelLookup(model.hash(), model.format())));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static synchronized ServerModelStore store(MinecraftServer server) {
        return STORES.computeIfAbsent(server, ServerModelStore::new);
    }

    private static boolean hasAdministratorRights(CommandSourceStack source) {
        return source.permissions() instanceof LevelBasedPermissionSet permissionSet
                && permissionSet.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
    }
}
