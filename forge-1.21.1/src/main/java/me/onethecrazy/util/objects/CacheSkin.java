package me.onethecrazy.util.objects;

import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.parsing.FBXParser;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CacheSkin {
    @Nullable
    public List<Vertex> vertices;
    @Nullable
    public SkinnedModel skinnedModel;
    public ParsingFormat format;

    public static CacheSkin empty(){
        return new CacheSkin(List.of(), null);
    }

    public CacheSkin(@Nullable List<Vertex> vertices, ParsingFormat format){
        this.format = format;
        this.vertices = vertices;
        this.skinnedModel = null;
    }

    public CacheSkin(@Nullable SkinnedModel skinnedModel, ParsingFormat format){
        this.format = format;
        this.vertices = null;
        this.skinnedModel = skinnedModel;
    }

    public String debugStatus() {
        if (skinnedModel != null) {
            return "Rig: skinned, bones=" + skinnedModel.bones.size()
                    + ", weighted=" + skinnedModel.weightedVertexCount() + "/" + skinnedModel.vertices.size()
                    + ", idle=" + skinnedModel.trackCount("Idle")
                    + ", walk=" + skinnedModel.trackCount("Walk")
                    + ", sneak=" + skinnedModel.trackCount("Sneak");
        }

        if (vertices != null && !vertices.isEmpty()) {
            return "Rig: static, vertices=" + vertices.size() + ", fbx=" + FBXParser.lastRigStatus()
                    + ", " + FBXParser.lastMaterialStatus();
        }

        return "Rig: none";
    }
}
