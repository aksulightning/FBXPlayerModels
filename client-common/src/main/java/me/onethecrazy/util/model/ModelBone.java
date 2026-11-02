package me.onethecrazy.util.model;

import org.joml.Matrix4f;

public class ModelBone {
    public String name;
    public int parentIndex;
    public Matrix4f bindTransform;
    public Matrix4f localTransform;

    public ModelBone(String name, int parentIndex, Matrix4f bindTransform, Matrix4f localTransform) {
        this.name = name;
        this.parentIndex = parentIndex;
        this.bindTransform = bindTransform;
        this.localTransform = localTransform;
    }
}
