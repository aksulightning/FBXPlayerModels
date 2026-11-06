package me.onethecrazy.util.model;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class ModelMaterial {
    public String name;
    public int baseColor;
    @Nullable public String texturePath;
    @Nullable public Identifier resolvedTexture;
    public String alphaMode;

    public ModelMaterial(String name, int baseColor, @Nullable String texturePath, @Nullable Identifier resolvedTexture, String alphaMode) {
        this.name = name;
        this.baseColor = baseColor;
        this.texturePath = texturePath;
        this.resolvedTexture = resolvedTexture;
        this.alphaMode = alphaMode;
    }
}
