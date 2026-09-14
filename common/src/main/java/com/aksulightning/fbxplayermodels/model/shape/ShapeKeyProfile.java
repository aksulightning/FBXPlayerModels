package com.aksulightning.fbxplayermodels.model.shape;

import java.util.LinkedHashMap;
import java.util.Map;

/** Saved weights are independent of geometry so further pose profiles can reuse the targets. */
public class ShapeKeyProfile {
    public static final String DEFAULT = "Default";
    public Map<String, Float> weights = new LinkedHashMap<>();

    public float weight(String id) {
        Float value = weights == null ? null : weights.get(id);
        return value == null ? 0f : clampWeight(value);
    }

    public void setWeight(String id, float weight) {
        if (weights == null) weights = new LinkedHashMap<>();
        weight = clampWeight(weight);
        if (weight == 0f) weights.remove(id);
        else weights.put(id, weight);
    }

    public void reset() {
        weights = new LinkedHashMap<>();
    }

    public static float clampWeight(float weight) {
        return Float.isFinite(weight) ? Math.max(0f, Math.min(1f, weight)) : 0f;
    }
}
