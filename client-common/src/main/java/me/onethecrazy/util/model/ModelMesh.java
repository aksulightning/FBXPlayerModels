package me.onethecrazy.util.model;

import java.util.ArrayList;
import java.util.List;

public class ModelMesh {
    public final List<ModelVertex> vertices = new ArrayList<>();
    public final List<Integer> indices = new ArrayList<>();
    public int materialId = -1;
    public int nodeId = -1;
}
