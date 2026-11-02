package me.onethecrazy.util.model;

import org.joml.Matrix4f;

public class ModelNode {
    public String name;
    public int parentIndex;
    public Matrix4f transform;

    public ModelNode(String name, int parentIndex, Matrix4f transform) {
        this.name = name;
        this.parentIndex = parentIndex;
        this.transform = transform;
    }
}
