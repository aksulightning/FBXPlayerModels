package com.aksulightning.fbxplayermodels.model.shape;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/** Additive deltas in model bind space, indexed like the expanded render vertices. */
public record ShapeKey(String id, String name, List<DeltaBlock> blocks) {
    public ShapeKey {
        blocks = List.copyOf(blocks);
    }

    /** Height normalization is a positive uniform scale and translation; unit normals stay unchanged. */
    public ShapeKey normalized(Matrix4f transform) {
        return new ShapeKey(id, name, blocks.stream().map(block -> new DeltaBlock(block.firstVertex(),
                transformDeltas(block.positions(), new Matrix3f(transform)),
                block.normals())).toList());
    }

    private static float[] transformDeltas(float[] source, Matrix3f transform) {
        float[] result = new float[source.length];
        for (int i = 0; i < source.length; i += 3) {
            Vector3f delta = transform.transform(new Vector3f(source[i], source[i + 1], source[i + 2]));
            result[i] = delta.x;
            result[i + 1] = delta.y;
            result[i + 2] = delta.z;
        }
        return result;
    }

    public record DeltaBlock(int firstVertex, float[] positions, float[] normals) {
        public DeltaBlock {
            if (firstVertex < 0 || positions.length % 3 != 0 || normals.length != positions.length) {
                throw new IllegalArgumentException("Invalid shape key delta block");
            }
        }
    }
}
