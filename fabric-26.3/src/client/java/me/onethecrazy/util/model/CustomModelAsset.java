package me.onethecrazy.util.model;

import me.onethecrazy.util.model.animation.ModelAnimationSet;
import me.onethecrazy.util.model.rig.LogicalRigBinding;
import me.onethecrazy.util.parsing.ParsingFormat;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CustomModelAsset {
    public final List<ModelNode> nodes = new ArrayList<>();
    public final List<ModelMesh> meshes = new ArrayList<>();
    public final List<ModelMaterial> materials = new ArrayList<>();
    @Nullable public ModelSkeleton skeleton;
    @Nullable public ModelAnimationSet animations;
    public LogicalRigBinding logicalRigBinding = new LogicalRigBinding();
    public ParsingFormat sourceFormat;
    public String sourceFileName = "";
    public float scale = 1f;
    public final List<String> importWarnings = new ArrayList<>();

    public CustomModelAsset(ParsingFormat sourceFormat, String sourceFileName) {
        this.sourceFormat = sourceFormat;
        this.sourceFileName = sourceFileName;
    }
}
