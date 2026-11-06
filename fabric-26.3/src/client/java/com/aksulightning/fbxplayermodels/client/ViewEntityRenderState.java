package com.aksulightning.fbxplayermodels.client;

import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.List;

public class ViewEntityRenderState extends EntityRenderState {
    public List<Vertex> vertices = List.of();
    public float yRot;
}
