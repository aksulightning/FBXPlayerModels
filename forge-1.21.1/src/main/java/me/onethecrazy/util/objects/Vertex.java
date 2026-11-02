package me.onethecrazy.util.objects;

import me.onethecrazy.FBXPlayerModels;
import net.minecraft.resources.ResourceLocation;

public class Vertex {
    public Float3 position;
    public Float3 normals;
    public Float2 textureUV;
    public int color;
    public ResourceLocation texture;

    public Vertex(Float3 position, Float3 normals, Float2 textureUV){
        this.position = position;
        this.normals = normals;
        this.textureUV = textureUV;
        this.texture = ResourceLocation.fromNamespaceAndPath(FBXPlayerModels.MOD_ID, "textures/white_pixel.png");
        this.color = 0xFFFFFFFF;
    }

    public Vertex(Float3 position, Float3 normals, Float2 textureUV, ResourceLocation texture){
        this.position = position;
        this.normals = normals;
        this.textureUV = textureUV;
        this.texture = texture;
        this.color = 0xFFFFFFFF;
    }

    public Vertex(Float3 position, Float3 normals, Float2 textureUV, int color){
        this.position = position;
        this.normals = normals;
        this.textureUV = textureUV;
        this.texture = ResourceLocation.fromNamespaceAndPath(FBXPlayerModels.MOD_ID, "textures/white_pixel.png");
        this.color = color;
    }

    public Vertex(Float3 position, Float3 normals, Float2 textureUV, ResourceLocation texture, int color){
        this.position = position;
        this.normals = normals;
        this.textureUV = textureUV;
        this.texture = texture;
        this.color = color;
    }
}
