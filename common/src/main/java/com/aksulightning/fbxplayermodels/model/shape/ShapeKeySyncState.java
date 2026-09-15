package com.aksulightning.fbxplayermodels.model.shape;

import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, model-specific Default and Voice Shape state that is safe to persist and relay through a server. */
public final class ShapeKeySyncState {
    public static final int MAX_WEIGHTS = 256;
    public static final int MAX_MODEL_HASH_LENGTH = 80;
    public static final int MAX_TARGET_ID_LENGTH = 512;
    public static final int MAX_TARGET_NAME_LENGTH = 256;

    public String modelHash = "";
    public Map<String, Float> defaultWeights = new LinkedHashMap<>();
    public VoiceShapeSettings voiceShapeSettings = new VoiceShapeSettings();

    public ShapeKeySyncState() {
    }

    public ShapeKeySyncState(String modelHash, Map<String, Float> defaultWeights,
                             VoiceShapeSettings voiceShapeSettings) {
        this.modelHash = modelHash;
        this.defaultWeights = defaultWeights;
        this.voiceShapeSettings = voiceShapeSettings;
    }

    public static ShapeKeySyncState empty(String modelHash) {
        return new ShapeKeySyncState(modelHash, Map.of(), new VoiceShapeSettings());
    }

    public static ShapeKeySyncState from(String modelHash, ShapeKeyProfile profile,
                                         VoiceShapeSettings voiceShapeSettings) {
        return new ShapeKeySyncState(modelHash,
                profile == null ? Map.of() : profile.weights,
                voiceShapeSettings).snapshot();
    }

    public ShapeKeySyncState snapshot() {
        String safeHash = bounded(modelHash, MAX_MODEL_HASH_LENGTH);
        Map<String, Float> safeWeights = new LinkedHashMap<>();
        if (defaultWeights != null) {
            for (Map.Entry<String, Float> entry : defaultWeights.entrySet()) {
                if (safeWeights.size() >= MAX_WEIGHTS) break;
                String id = entry.getKey();
                Float value = entry.getValue();
                if (id == null || id.isBlank() || id.length() > MAX_TARGET_ID_LENGTH || value == null) continue;
                float weight = ShapeKeyProfile.clampWeight(value);
                if (weight != 0f) safeWeights.put(id, weight);
            }
        }

        VoiceShapeSettings safeVoice = voiceShapeSettings == null
                ? new VoiceShapeSettings() : voiceShapeSettings.snapshot();
        safeVoice.name = bounded(safeVoice.name, MAX_TARGET_NAME_LENGTH);
        safeVoice.shapeKeyId = bounded(safeVoice.shapeKeyId, MAX_TARGET_ID_LENGTH);
        switch (safeVoice.target()) {
            case NONE -> {
                safeVoice.name = "";
                safeVoice.shapeKeyId = "";
            }
            case BONE -> {
                safeVoice.shapeKeyId = "";
                if (safeVoice.name.isBlank()) safeVoice.target = VoiceShapeSettings.Target.NONE;
            }
            case SHAPE_KEY -> {
                if (safeVoice.name.isBlank() || safeVoice.shapeKeyId.isBlank()) {
                    safeVoice.target = VoiceShapeSettings.Target.NONE;
                    safeVoice.name = "";
                    safeVoice.shapeKeyId = "";
                }
            }
        }
        return new ShapeKeySyncState(safeHash, Map.copyOf(safeWeights), safeVoice);
    }

    public ShapeKeyProfile defaultProfile() {
        ShapeKeyProfile profile = new ShapeKeyProfile();
        profile.weights = new LinkedHashMap<>(snapshot().defaultWeights);
        return profile;
    }

    private static String bounded(String value, int maxLength) {
        if (value == null || value.length() > maxLength) return "";
        return value;
    }
}
