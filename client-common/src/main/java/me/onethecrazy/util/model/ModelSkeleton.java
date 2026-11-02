package me.onethecrazy.util.model;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ModelSkeleton {
    public final List<ModelBone> bones = new ArrayList<>();
    public final List<Matrix4f> inverseBindMatrices = new ArrayList<>();
}
