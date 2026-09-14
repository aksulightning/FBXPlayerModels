package com.aksulightning.fbxplayermodels.model.shape;

import me.onethecrazy.util.objects.Float2;
import me.onethecrazy.util.objects.Float3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Source FBX control-point identities survive material splits and render-vertex duplication. */
public record FbxShapeKeyGeometry(long geometryId, String name, List<Float3> controlPoints,
                                  List<Corner> corners, List<Target> targets) {
    public record Corner(int controlPoint, Float2 uv) {}
    public record Target(String id, String name, float[] positions, float[] normals) {}

    public Optional<List<Integer>> mapControlPoints(List<Float3> positions, List<Float2> uvs) {
        Map<VertexKey, List<Integer>> byVertex = new HashMap<>();
        Map<PositionKey, List<Integer>> byPosition = new HashMap<>();
        for (Corner corner : corners) {
            if (corner.controlPoint < 0 || corner.controlPoint >= controlPoints.size()) continue;
            Float3 position = controlPoints.get(corner.controlPoint);
            byVertex.computeIfAbsent(VertexKey.of(position, corner.uv), ignored -> new ArrayList<>()).add(corner.controlPoint);
        }
        for (int i = 0; i < controlPoints.size(); i++) {
            byPosition.computeIfAbsent(PositionKey.of(controlPoints.get(i)), ignored -> new ArrayList<>()).add(i);
        }
        List<Integer> result = new ArrayList<>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            List<Integer> candidates = byVertex.get(VertexKey.of(positions.get(i), uvs.get(i)));
            if (candidates == null) candidates = byPosition.get(PositionKey.of(positions.get(i)));
            if (candidates == null || candidates.isEmpty()) return Optional.empty();
            int selected = candidates.getFirst();
            for (int candidate : candidates) {
                if (!sameDeltas(selected, candidate)) return Optional.empty();
            }
            result.add(selected);
        }
        return Optional.of(result);
    }

    private boolean sameDeltas(int first, int second) {
        if (first == second) return true;
        for (Target target : targets) {
            for (int axis = 0; axis < 3; axis++) {
                if (target.positions[first * 3 + axis] != target.positions[second * 3 + axis]
                        || target.normals[first * 3 + axis] != target.normals[second * 3 + axis]) return false;
            }
        }
        return true;
    }

    public List<ShapeKey> expand(int firstVertex, List<Integer> controlIndices, List<Float3> baseNormals, Matrix4f transform) {
        List<ShapeKey> result = new ArrayList<>();
        Matrix3f normalTransform = transform.normal(new Matrix3f());
        for (Target target : targets) {
            float[] positions = new float[controlIndices.size() * 3];
            float[] normals = new float[positions.length];
            for (int i = 0; i < controlIndices.size(); i++) {
                int source = controlIndices.get(i) * 3;
                int at = i * 3;
                Vector3f delta = transform.transformDirection(new Vector3f(
                        target.positions[source], target.positions[source + 1], target.positions[source + 2]));
                positions[at] = delta.x;
                positions[at + 1] = delta.y;
                positions[at + 2] = delta.z;
                Float3 base = baseNormals.get(i);
                Vector3f baseNormal = normalTransform.transform(new Vector3f(base.x, base.y, base.z));
                Vector3f targetNormal = normalTransform.transform(new Vector3f(base.x, base.y, base.z).add(
                        target.normals[source], target.normals[source + 1], target.normals[source + 2]));
                if (baseNormal.lengthSquared() > 0f) baseNormal.normalize();
                if (targetNormal.lengthSquared() > 0f) targetNormal.normalize();
                targetNormal.sub(baseNormal);
                normals[at] = targetNormal.x;
                normals[at + 1] = targetNormal.y;
                normals[at + 2] = targetNormal.z;
            }
            result.add(new ShapeKey(target.id, target.name, List.of(new ShapeKey.DeltaBlock(firstVertex, positions, normals))));
        }
        return result;
    }

    private record PositionKey(float x, float y, float z) {
        static PositionKey of(Float3 position) {
            return new PositionKey(canonical(position.x), canonical(position.y), canonical(position.z));
        }
    }

    private record VertexKey(PositionKey position, float u, float v) {
        static VertexKey of(Float3 position, Float2 uv) {
            return new VertexKey(PositionKey.of(position), canonical(uv.u), canonical(uv.v));
        }
    }

    private static float canonical(float value) {
        return value == 0f ? 0f : value;
    }
}
