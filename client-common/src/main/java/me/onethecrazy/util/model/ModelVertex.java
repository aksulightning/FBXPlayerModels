package me.onethecrazy.util.model;

import me.onethecrazy.util.objects.Float2;
import me.onethecrazy.util.objects.Float3;

public class ModelVertex {
    public Float3 position = Float3.empty();
    public Float3 normal = Float3.empty();
    public Float3 tangent = Float3.empty();
    public Float2 uv = Float2.empty();
    public int color = 0xFFFFFFFF;
    public int[] boneIds = new int[0];
    public float[] boneWeights = new float[0];
}
