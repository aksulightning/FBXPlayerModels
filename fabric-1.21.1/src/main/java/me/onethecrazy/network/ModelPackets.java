package me.onethecrazy.network;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKeySyncState;
import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;
import me.onethecrazy.FBXPlayerModels;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
        return Identifier.of(FBXPlayerModels.MOD_ID, path);
    }

    public record UploadModelPayload(String fileName, String format, byte[] data) implements CustomPayload {
        public static final Id<UploadModelPayload> ID = new Id<>(id("upload_model"));
        public static final PacketCodec<RegistryByteBuf, UploadModelPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeUploadModel,
                ModelPackets::readUploadModel
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RequestLookupPayload(List<String> uuids) implements CustomPayload {
        public static final Id<RequestLookupPayload> ID = new Id<>(id("request_lookup"));
        public static final PacketCodec<RegistryByteBuf, RequestLookupPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeRequestLookup,
                ModelPackets::readRequestLookup
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RequestModelPayload(String hash, String format) implements CustomPayload {
        public static final Id<RequestModelPayload> ID = new Id<>(id("request_model"));
        public static final PacketCodec<RegistryByteBuf, RequestModelPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeRequestModel,
                ModelPackets::readRequestModel
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RequestMobModelPayload(String model) implements CustomPayload {
        public static final Id<RequestMobModelPayload> ID = new Id<>(id("request_mob_model"));
        public static final PacketCodec<RegistryByteBuf, RequestMobModelPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeRequestMobModel,
                ModelPackets::readRequestMobModel
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record LookupResponsePayload(Map<String, ModelLookup> models) implements CustomPayload {
        public static final Id<LookupResponsePayload> ID = new Id<>(id("lookup_response"));
        public static final PacketCodec<RegistryByteBuf, LookupResponsePayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeLookupResponse,
                ModelPackets::readLookupResponse
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ModelDataPayload(String hash, String format, byte[] data, String message) implements CustomPayload {
        public static final Id<ModelDataPayload> ID = new Id<>(id("model_data"));
        public static final PacketCodec<RegistryByteBuf, ModelDataPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeModelData,
                ModelPackets::readModelData
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record MobModelDataPayload(String model, byte[] data, String message) implements CustomPayload {
        public static final Id<MobModelDataPayload> ID = new Id<>(id("mob_model_data"));
        public static final PacketCodec<RegistryByteBuf, MobModelDataPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeMobModelData,
                ModelPackets::readMobModelData
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UploadResultPayload(boolean success, String message) implements CustomPayload {
        public static final Id<UploadResultPayload> ID = new Id<>(id("upload_result"));
        public static final PacketCodec<RegistryByteBuf, UploadResultPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeUploadResult,
                ModelPackets::readUploadResult
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateShapeSettingsPayload(ShapeKeySyncState settings) implements CustomPayload {
        public static final Id<UpdateShapeSettingsPayload> ID = new Id<>(id("update_shape_settings"));
        public static final PacketCodec<RegistryByteBuf, UpdateShapeSettingsPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeUpdateShapeSettings,
                ModelPackets::readUpdateShapeSettings
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PlayerShapeSettingsPayload(String uuid, ShapeKeySyncState settings) implements CustomPayload {
        public static final Id<PlayerShapeSettingsPayload> ID = new Id<>(id("player_shape_settings"));
        public static final PacketCodec<RegistryByteBuf, PlayerShapeSettingsPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writePlayerShapeSettings,
                ModelPackets::readPlayerShapeSettings
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record VoiceLevelPayload(float level) implements CustomPayload {
        public static final Id<VoiceLevelPayload> ID = new Id<>(id("voice_level"));
        public static final PacketCodec<RegistryByteBuf, VoiceLevelPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writeVoiceLevel,
                ModelPackets::readVoiceLevel
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PlayerVoiceLevelPayload(String uuid, float level) implements CustomPayload {
        public static final Id<PlayerVoiceLevelPayload> ID = new Id<>(id("player_voice_level"));
        public static final PacketCodec<RegistryByteBuf, PlayerVoiceLevelPayload> CODEC = PacketCodec.ofStatic(
                ModelPackets::writePlayerVoiceLevel,
                ModelPackets::readPlayerVoiceLevel
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ModelLookup(String hash, String format) {
    }

    private static UploadModelPayload readUploadModel(RegistryByteBuf buf) {
        String fileName = buf.readString(160);
        String format = buf.readString(16);
        byte[] data = buf.readByteArray(MAX_MODEL_BYTES + 1);
        return new UploadModelPayload(fileName, format, data);
    }

    private static void writeUploadModel(RegistryByteBuf buf, UploadModelPayload payload) {
        buf.writeString(payload.fileName(), 160);
        buf.writeString(payload.format(), 16);
        buf.writeByteArray(payload.data());
    }

    private static RequestLookupPayload readRequestLookup(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 128) {
            throw new IllegalArgumentException("Invalid lookup request size");
        }
        List<String> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(buf.readString(64));
        }
        return new RequestLookupPayload(uuids);
    }

    private static void writeRequestLookup(RegistryByteBuf buf, RequestLookupPayload payload) {
        buf.writeVarInt(payload.uuids().size());
        for (String uuid : payload.uuids()) {
            buf.writeString(uuid, 64);
        }
    }

    private static RequestModelPayload readRequestModel(RegistryByteBuf buf) {
        return new RequestModelPayload(buf.readString(80), buf.readString(16));
    }

    private static void writeRequestModel(RegistryByteBuf buf, RequestModelPayload payload) {
        buf.writeString(payload.hash(), 80);
        buf.writeString(payload.format(), 16);
    }

    private static RequestMobModelPayload readRequestMobModel(RegistryByteBuf buf) {
        return new RequestMobModelPayload(buf.readString(160));
    }

    private static void writeRequestMobModel(RegistryByteBuf buf, RequestMobModelPayload payload) {
        buf.writeString(payload.model(), 160);
    }

    private static LookupResponsePayload readLookupResponse(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 128) {
            throw new IllegalArgumentException("Invalid lookup response size");
        }
        Map<String, ModelLookup> models = new HashMap<>();
        for (int i = 0; i < count; i++) {
            models.put(buf.readString(64), new ModelLookup(buf.readString(80), buf.readString(16)));
        }
        return new LookupResponsePayload(models);
    }

    private static void writeLookupResponse(RegistryByteBuf buf, LookupResponsePayload payload) {
        buf.writeVarInt(payload.models().size());
        for (Map.Entry<String, ModelLookup> entry : payload.models().entrySet()) {
            buf.writeString(entry.getKey(), 64);
            buf.writeString(entry.getValue().hash(), 80);
            buf.writeString(entry.getValue().format(), 16);
        }
    }

    private static ModelDataPayload readModelData(RegistryByteBuf buf) {
        return new ModelDataPayload(buf.readString(80), buf.readString(16), buf.readByteArray(MAX_MODEL_BYTES), buf.readString(256));
    }

    private static void writeModelData(RegistryByteBuf buf, ModelDataPayload payload) {
        buf.writeString(payload.hash(), 80);
        buf.writeString(payload.format(), 16);
        buf.writeByteArray(payload.data());
        buf.writeString(payload.message(), 256);
    }

    private static MobModelDataPayload readMobModelData(RegistryByteBuf buf) {
        return new MobModelDataPayload(buf.readString(160), buf.readByteArray(MAX_MODEL_BYTES), buf.readString(256));
    }

    private static void writeMobModelData(RegistryByteBuf buf, MobModelDataPayload payload) {
        buf.writeString(payload.model(), 160);
        buf.writeByteArray(payload.data());
        buf.writeString(payload.message(), 256);
    }

    private static UploadResultPayload readUploadResult(PacketByteBuf buf) {
        return new UploadResultPayload(buf.readBoolean(), buf.readString(256));
    }

    private static void writeUploadResult(PacketByteBuf buf, UploadResultPayload payload) {
        buf.writeBoolean(payload.success());
        buf.writeString(payload.message(), 256);
    }

    private static UpdateShapeSettingsPayload readUpdateShapeSettings(RegistryByteBuf buf) {
        return new UpdateShapeSettingsPayload(readShapeSettings(buf));
    }

    private static void writeUpdateShapeSettings(RegistryByteBuf buf, UpdateShapeSettingsPayload payload) {
        writeShapeSettings(buf, payload.settings());
    }

    private static PlayerShapeSettingsPayload readPlayerShapeSettings(RegistryByteBuf buf) {
        return new PlayerShapeSettingsPayload(buf.readString(64), readShapeSettings(buf));
    }

    private static void writePlayerShapeSettings(RegistryByteBuf buf, PlayerShapeSettingsPayload payload) {
        buf.writeString(payload.uuid(), 64);
        writeShapeSettings(buf, payload.settings());
    }

    private static VoiceLevelPayload readVoiceLevel(RegistryByteBuf buf) {
        return new VoiceLevelPayload(buf.readFloat());
    }

    private static void writeVoiceLevel(RegistryByteBuf buf, VoiceLevelPayload payload) {
        buf.writeFloat(payload.level());
    }

    private static PlayerVoiceLevelPayload readPlayerVoiceLevel(RegistryByteBuf buf) {
        return new PlayerVoiceLevelPayload(buf.readString(64), buf.readFloat());
    }

    private static void writePlayerVoiceLevel(RegistryByteBuf buf, PlayerVoiceLevelPayload payload) {
        buf.writeString(payload.uuid(), 64);
        buf.writeFloat(payload.level());
    }

    private static ShapeKeySyncState readShapeSettings(RegistryByteBuf buf) {
        String modelHash = buf.readString(ShapeKeySyncState.MAX_MODEL_HASH_LENGTH);
        int count = buf.readVarInt();
        if (count < 0 || count > ShapeKeySyncState.MAX_WEIGHTS) {
            throw new IllegalArgumentException("Invalid shape key weight count");
        }
        Map<String, Float> weights = new HashMap<>();
        for (int i = 0; i < count; i++) {
            weights.put(buf.readString(ShapeKeySyncState.MAX_TARGET_ID_LENGTH), buf.readFloat());
        }
        return new ShapeKeySyncState(modelHash, weights, readVoiceSettings(buf)).snapshot();
    }

    private static void writeShapeSettings(RegistryByteBuf buf, ShapeKeySyncState settings) {
        ShapeKeySyncState safe = settings == null ? ShapeKeySyncState.empty("") : settings.snapshot();
        buf.writeString(safe.modelHash, ShapeKeySyncState.MAX_MODEL_HASH_LENGTH);
        buf.writeVarInt(safe.defaultWeights.size());
        for (Map.Entry<String, Float> entry : safe.defaultWeights.entrySet()) {
            buf.writeString(entry.getKey(), ShapeKeySyncState.MAX_TARGET_ID_LENGTH);
            buf.writeFloat(entry.getValue());
        }
        writeVoiceSettings(buf, safe.voiceShapeSettings);
    }

    private static VoiceShapeSettings readVoiceSettings(RegistryByteBuf buf) {
        VoiceShapeSettings settings = new VoiceShapeSettings();
        settings.target = readEnum(buf, VoiceShapeSettings.Target.values(), "voice target");
        settings.name = buf.readString(ShapeKeySyncState.MAX_TARGET_NAME_LENGTH);
        settings.shapeKeyId = buf.readString(ShapeKeySyncState.MAX_TARGET_ID_LENGTH);
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

    private static void writeVoiceSettings(RegistryByteBuf buf, VoiceShapeSettings settings) {
        VoiceShapeSettings safe = settings == null ? new VoiceShapeSettings() : settings.snapshot();
        buf.writeVarInt(safe.target().ordinal());
        buf.writeString(safe.name, ShapeKeySyncState.MAX_TARGET_NAME_LENGTH);
        buf.writeString(safe.shapeKeyId, ShapeKeySyncState.MAX_TARGET_ID_LENGTH);
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

    private static <T> T readEnum(RegistryByteBuf buf, T[] values, String label) {
        int index = buf.readVarInt();
        if (index < 0 || index >= values.length) throw new IllegalArgumentException("Invalid " + label);
        return values[index];
    }
}
