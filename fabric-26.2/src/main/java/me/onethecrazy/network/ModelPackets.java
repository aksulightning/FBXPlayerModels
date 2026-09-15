package me.onethecrazy.network;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKeySyncState;
import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;
import me.onethecrazy.FBXPlayerModels;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModelPackets {
    public static final int MODEL_SIZE_LIMIT_BYTES = 2 * 1024 * 1024;
    public static final int MAX_MODEL_BYTES = MODEL_SIZE_LIMIT_BYTES - 1;
    public static final int MAX_SHAPE_SETTINGS_BYTES = 512 * 1024;

    private ModelPackets() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FBXPlayerModels.MOD_ID, path);
    }

    public record UploadModelPayload(String fileName, String format, byte[] data) implements CustomPacketPayload {
        public static final Type<UploadModelPayload> TYPE = new Type<>(id("upload_model"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UploadModelPayload> CODEC = StreamCodec.of(
                ModelPackets::writeUploadModel,
                ModelPackets::readUploadModel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestLookupPayload(List<String> uuids) implements CustomPacketPayload {
        public static final Type<RequestLookupPayload> TYPE = new Type<>(id("request_lookup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestLookupPayload> CODEC = StreamCodec.of(
                ModelPackets::writeRequestLookup,
                ModelPackets::readRequestLookup
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestModelPayload(String hash, String format) implements CustomPacketPayload {
        public static final Type<RequestModelPayload> TYPE = new Type<>(id("request_model"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestModelPayload> CODEC = StreamCodec.of(
                ModelPackets::writeRequestModel,
                ModelPackets::readRequestModel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestMobModelPayload(String model) implements CustomPacketPayload {
        public static final Type<RequestMobModelPayload> TYPE = new Type<>(id("request_mob_model"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestMobModelPayload> CODEC = StreamCodec.of(
                ModelPackets::writeRequestMobModel,
                ModelPackets::readRequestMobModel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LookupResponsePayload(Map<String, ModelLookup> models) implements CustomPacketPayload {
        public static final Type<LookupResponsePayload> TYPE = new Type<>(id("lookup_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LookupResponsePayload> CODEC = StreamCodec.of(
                ModelPackets::writeLookupResponse,
                ModelPackets::readLookupResponse
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ModelDataPayload(String hash, String format, byte[] data, String message) implements CustomPacketPayload {
        public static final Type<ModelDataPayload> TYPE = new Type<>(id("model_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModelDataPayload> CODEC = StreamCodec.of(
                ModelPackets::writeModelData,
                ModelPackets::readModelData
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MobModelDataPayload(String model, byte[] data, String message) implements CustomPacketPayload {
        public static final Type<MobModelDataPayload> TYPE = new Type<>(id("mob_model_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MobModelDataPayload> CODEC = StreamCodec.of(
                ModelPackets::writeMobModelData,
                ModelPackets::readMobModelData
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UploadResultPayload(boolean success, String message) implements CustomPacketPayload {
        public static final Type<UploadResultPayload> TYPE = new Type<>(id("upload_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UploadResultPayload> CODEC = StreamCodec.of(
                ModelPackets::writeUploadResult,
                ModelPackets::readUploadResult
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateShapeSettingsPayload(ShapeKeySyncState settings) implements CustomPacketPayload {
        public static final Type<UpdateShapeSettingsPayload> TYPE = new Type<>(id("update_shape_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateShapeSettingsPayload> CODEC = StreamCodec.of(
                ModelPackets::writeUpdateShapeSettings,
                ModelPackets::readUpdateShapeSettings
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayerShapeSettingsPayload(String uuid, ShapeKeySyncState settings) implements CustomPacketPayload {
        public static final Type<PlayerShapeSettingsPayload> TYPE = new Type<>(id("player_shape_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerShapeSettingsPayload> CODEC = StreamCodec.of(
                ModelPackets::writePlayerShapeSettings,
                ModelPackets::readPlayerShapeSettings
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record VoiceLevelPayload(float level) implements CustomPacketPayload {
        public static final Type<VoiceLevelPayload> TYPE = new Type<>(id("voice_level"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VoiceLevelPayload> CODEC = StreamCodec.of(
                ModelPackets::writeVoiceLevel,
                ModelPackets::readVoiceLevel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayerVoiceLevelPayload(String uuid, float level) implements CustomPacketPayload {
        public static final Type<PlayerVoiceLevelPayload> TYPE = new Type<>(id("player_voice_level"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVoiceLevelPayload> CODEC = StreamCodec.of(
                ModelPackets::writePlayerVoiceLevel,
                ModelPackets::readPlayerVoiceLevel
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ModelLookup(String hash, String format) {
    }

    private static UploadModelPayload readUploadModel(RegistryFriendlyByteBuf buf) {
        return new UploadModelPayload(buf.readUtf(160), buf.readUtf(16), buf.readByteArray(MAX_MODEL_BYTES + 1));
    }

    private static void writeUploadModel(RegistryFriendlyByteBuf buf, UploadModelPayload payload) {
        buf.writeUtf(payload.fileName(), 160);
        buf.writeUtf(payload.format(), 16);
        buf.writeByteArray(payload.data());
    }

    private static RequestLookupPayload readRequestLookup(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 128) {
            throw new IllegalArgumentException("Invalid lookup request size");
        }
        List<String> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(buf.readUtf(64));
        }
        return new RequestLookupPayload(uuids);
    }

    private static void writeRequestLookup(RegistryFriendlyByteBuf buf, RequestLookupPayload payload) {
        buf.writeVarInt(payload.uuids().size());
        for (String uuid : payload.uuids()) {
            buf.writeUtf(uuid, 64);
        }
    }

    private static RequestModelPayload readRequestModel(RegistryFriendlyByteBuf buf) {
        return new RequestModelPayload(buf.readUtf(80), buf.readUtf(16));
    }

    private static void writeRequestModel(RegistryFriendlyByteBuf buf, RequestModelPayload payload) {
        buf.writeUtf(payload.hash(), 80);
        buf.writeUtf(payload.format(), 16);
    }

    private static RequestMobModelPayload readRequestMobModel(RegistryFriendlyByteBuf buf) {
        return new RequestMobModelPayload(buf.readUtf(160));
    }

    private static void writeRequestMobModel(RegistryFriendlyByteBuf buf, RequestMobModelPayload payload) {
        buf.writeUtf(payload.model(), 160);
    }

    private static LookupResponsePayload readLookupResponse(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 128) {
            throw new IllegalArgumentException("Invalid lookup response size");
        }
        Map<String, ModelLookup> models = new HashMap<>();
        for (int i = 0; i < count; i++) {
            models.put(buf.readUtf(64), new ModelLookup(buf.readUtf(80), buf.readUtf(16)));
        }
        return new LookupResponsePayload(models);
    }

    private static void writeLookupResponse(RegistryFriendlyByteBuf buf, LookupResponsePayload payload) {
        buf.writeVarInt(payload.models().size());
        for (Map.Entry<String, ModelLookup> entry : payload.models().entrySet()) {
            buf.writeUtf(entry.getKey(), 64);
            buf.writeUtf(entry.getValue().hash(), 80);
            buf.writeUtf(entry.getValue().format(), 16);
        }
    }

    private static ModelDataPayload readModelData(RegistryFriendlyByteBuf buf) {
        return new ModelDataPayload(buf.readUtf(80), buf.readUtf(16), buf.readByteArray(MAX_MODEL_BYTES), buf.readUtf(256));
    }

    private static void writeModelData(RegistryFriendlyByteBuf buf, ModelDataPayload payload) {
        buf.writeUtf(payload.hash(), 80);
        buf.writeUtf(payload.format(), 16);
        buf.writeByteArray(payload.data());
        buf.writeUtf(payload.message(), 256);
    }

    private static MobModelDataPayload readMobModelData(RegistryFriendlyByteBuf buf) {
        return new MobModelDataPayload(buf.readUtf(160), buf.readByteArray(MAX_MODEL_BYTES), buf.readUtf(256));
    }

    private static void writeMobModelData(RegistryFriendlyByteBuf buf, MobModelDataPayload payload) {
        buf.writeUtf(payload.model(), 160);
        buf.writeByteArray(payload.data());
        buf.writeUtf(payload.message(), 256);
    }

    private static UploadResultPayload readUploadResult(RegistryFriendlyByteBuf buf) {
        return new UploadResultPayload(buf.readBoolean(), buf.readUtf(256));
    }

    private static void writeUploadResult(RegistryFriendlyByteBuf buf, UploadResultPayload payload) {
        buf.writeBoolean(payload.success());
        buf.writeUtf(payload.message(), 256);
    }

    private static UpdateShapeSettingsPayload readUpdateShapeSettings(RegistryFriendlyByteBuf buf) {
        return new UpdateShapeSettingsPayload(readShapeSettings(buf));
    }

    private static void writeUpdateShapeSettings(RegistryFriendlyByteBuf buf, UpdateShapeSettingsPayload payload) {
        writeShapeSettings(buf, payload.settings());
    }

    private static PlayerShapeSettingsPayload readPlayerShapeSettings(RegistryFriendlyByteBuf buf) {
        return new PlayerShapeSettingsPayload(buf.readUtf(64), readShapeSettings(buf));
    }

    private static void writePlayerShapeSettings(RegistryFriendlyByteBuf buf, PlayerShapeSettingsPayload payload) {
        buf.writeUtf(payload.uuid(), 64);
        writeShapeSettings(buf, payload.settings());
    }

    private static VoiceLevelPayload readVoiceLevel(RegistryFriendlyByteBuf buf) {
        return new VoiceLevelPayload(buf.readFloat());
    }

    private static void writeVoiceLevel(RegistryFriendlyByteBuf buf, VoiceLevelPayload payload) {
        buf.writeFloat(payload.level());
    }

    private static PlayerVoiceLevelPayload readPlayerVoiceLevel(RegistryFriendlyByteBuf buf) {
        return new PlayerVoiceLevelPayload(buf.readUtf(64), buf.readFloat());
    }

    private static void writePlayerVoiceLevel(RegistryFriendlyByteBuf buf, PlayerVoiceLevelPayload payload) {
        buf.writeUtf(payload.uuid(), 64);
        buf.writeFloat(payload.level());
    }

    private static ShapeKeySyncState readShapeSettings(RegistryFriendlyByteBuf buf) {
        String modelHash = buf.readUtf(ShapeKeySyncState.MAX_MODEL_HASH_LENGTH);
        int count = buf.readVarInt();
        if (count < 0 || count > ShapeKeySyncState.MAX_WEIGHTS) {
            throw new IllegalArgumentException("Invalid shape key weight count");
        }
        Map<String, Float> weights = new HashMap<>();
        for (int i = 0; i < count; i++) {
            weights.put(buf.readUtf(ShapeKeySyncState.MAX_TARGET_ID_LENGTH), buf.readFloat());
        }
        return new ShapeKeySyncState(modelHash, weights, readVoiceSettings(buf)).snapshot();
    }

    private static void writeShapeSettings(RegistryFriendlyByteBuf buf, ShapeKeySyncState settings) {
        ShapeKeySyncState safe = settings == null ? ShapeKeySyncState.empty("") : settings.snapshot();
        buf.writeUtf(safe.modelHash, ShapeKeySyncState.MAX_MODEL_HASH_LENGTH);
        buf.writeVarInt(safe.defaultWeights.size());
        for (Map.Entry<String, Float> entry : safe.defaultWeights.entrySet()) {
            buf.writeUtf(entry.getKey(), ShapeKeySyncState.MAX_TARGET_ID_LENGTH);
            buf.writeFloat(entry.getValue());
        }
        writeVoiceSettings(buf, safe.voiceShapeSettings);
    }

    private static VoiceShapeSettings readVoiceSettings(RegistryFriendlyByteBuf buf) {
        VoiceShapeSettings settings = new VoiceShapeSettings();
        settings.target = readEnum(buf, VoiceShapeSettings.Target.values(), "voice target");
        settings.name = buf.readUtf(ShapeKeySyncState.MAX_TARGET_NAME_LENGTH);
        settings.shapeKeyId = buf.readUtf(ShapeKeySyncState.MAX_TARGET_ID_LENGTH);
        settings.input = readEnum(buf, VoiceShapeSettings.Input.values(), "voice input");
        settings.response = readEnum(buf, VoiceShapeSettings.Response.values(), "voice response");
        settings.startX = buf.readFloat();
        settings.startY = buf.readFloat();
        settings.startZ = buf.readFloat();
        settings.endX = buf.readFloat();
        settings.endY = buf.readFloat();
        settings.endZ = buf.readFloat();
        settings.startWeight = buf.readFloat();
        settings.endWeight = buf.readFloat();
        settings.sensitivity = buf.readFloat();
        settings.threshold = buf.readFloat();
        return settings.snapshot();
    }

    private static void writeVoiceSettings(RegistryFriendlyByteBuf buf, VoiceShapeSettings settings) {
        VoiceShapeSettings safe = settings == null ? new VoiceShapeSettings() : settings.snapshot();
        buf.writeVarInt(safe.target().ordinal());
        buf.writeUtf(safe.name, ShapeKeySyncState.MAX_TARGET_NAME_LENGTH);
        buf.writeUtf(safe.shapeKeyId, ShapeKeySyncState.MAX_TARGET_ID_LENGTH);
        buf.writeVarInt(safe.input().ordinal());
        buf.writeVarInt(safe.response().ordinal());
        buf.writeFloat(safe.startX);
        buf.writeFloat(safe.startY);
        buf.writeFloat(safe.startZ);
        buf.writeFloat(safe.endX);
        buf.writeFloat(safe.endY);
        buf.writeFloat(safe.endZ);
        buf.writeFloat(safe.startWeight);
        buf.writeFloat(safe.endWeight);
        buf.writeFloat(safe.sensitivity);
        buf.writeFloat(safe.threshold);
    }

    private static <T> T readEnum(RegistryFriendlyByteBuf buf, T[] values, String label) {
        int index = buf.readVarInt();
        if (index < 0 || index >= values.length) throw new IllegalArgumentException("Invalid " + label);
        return values[index];
    }
}
