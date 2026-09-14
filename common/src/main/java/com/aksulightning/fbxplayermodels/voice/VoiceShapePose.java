package com.aksulightning.fbxplayermodels.voice;

/** Immutable per-frame input, also safe to carry through deferred render states. */
public record VoiceShapePose(String boneName, String shapeKeyId, float x, float y, float z, float weight) {
    public static final VoiceShapePose NONE = new VoiceShapePose("", "", 0f, 0f, 0f, 0f);

    public static VoiceShapePose from(VoiceShapeSettings settings, float amount) {
        if (!settings.enabled()) return NONE;
        VoiceShapeSettings s = settings.snapshot();
        float t = VoiceShapeSettings.clamp(amount, 0f, 1f);
        if (s.target == VoiceShapeSettings.Target.BONE) {
            return new VoiceShapePose(s.name, "", mix(s.startX, s.endX, t),
                    mix(s.startY, s.endY, t), mix(s.startZ, s.endZ, t), 0f);
        }
        if (s.shapeKeyId.isBlank()) return NONE;
        return new VoiceShapePose("", s.shapeKeyId, 0f, 0f, 0f, mix(s.startWeight, s.endWeight, t));
    }

    private static float mix(float start, float end, float t) { return start + (end - start) * t; }
    public boolean isNone() { return boneName.isEmpty() && shapeKeyId.isEmpty(); }
}
