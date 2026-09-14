package com.aksulightning.fbxplayermodels.voice;

/** One experimental voice mapping per local model, independent of its default shape profile. */
public final class VoiceShapeSettings {
    public enum Target { NONE, BONE, SHAPE_KEY }
    public enum Input { AUTO, VOICE_CHAT, MICROPHONE }
    public enum Response { VOLUME, RHYTHM, TRIGGER }

    public Target target = Target.NONE;
    public String name = "";
    public String shapeKeyId = "";
    public Input input = Input.AUTO;
    public Response response = Response.VOLUME;
    public float startX, startY, startZ;
    public float endX, endY, endZ;
    public float startWeight;
    public float endWeight = 1f;
    public float sensitivity = 8f;
    public float threshold = 0.08f;

    public Target target() { return target == null ? Target.NONE : target; }
    public Input input() { return input == null ? Input.AUTO : input; }
    public Response response() { return response == null ? Response.VOLUME : response; }
    public boolean enabled() { return target() != Target.NONE && name != null && !name.isBlank(); }

    public VoiceShapeSettings snapshot() {
        VoiceShapeSettings copy = new VoiceShapeSettings();
        copy.target = target();
        copy.name = name == null ? "" : name;
        copy.shapeKeyId = shapeKeyId == null ? "" : shapeKeyId;
        copy.input = input();
        copy.response = response();
        copy.startX = clamp(startX, -180f, 180f);
        copy.startY = clamp(startY, -180f, 180f);
        copy.startZ = clamp(startZ, -180f, 180f);
        copy.endX = clamp(endX, -180f, 180f);
        copy.endY = clamp(endY, -180f, 180f);
        copy.endZ = clamp(endZ, -180f, 180f);
        copy.startWeight = clamp(startWeight, 0f, 1f);
        copy.endWeight = clamp(endWeight, 0f, 1f);
        copy.sensitivity = clamp(sensitivity, 1f, 30f);
        copy.threshold = clamp(threshold, 0f, 0.95f);
        return copy;
    }

    public static float clamp(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
