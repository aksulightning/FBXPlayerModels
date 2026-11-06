package com.aksulightning.fbxplayermodels.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.aksulightning.fbxplayermodels.ViewEntity;
import com.aksulightning.fbxplayermodels.FbxModelEntity;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.List;

public class ViewEntityRenderer<T extends Entity & FbxModelEntity> extends EntityRenderer<T, ViewEntityRenderState> {
    public ViewEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ViewEntityRenderState createRenderState() {
        return new ViewEntityRenderState();
    }

    @Override
    public void extractRenderState(T entity, ViewEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.yRot = entity.getYRot(tickDelta);
        state.vertices = List.of();

        CacheSkin model = ViewEntityModelCache.get(entity.getModel());
        if (model == null) {
            return;
        }

        List<Vertex> vertices = model.vertices;
        if (model.skinnedModel != null) {
            float seconds = (entity.tickCount + tickDelta) / 20f;
            vertices = model.skinnedModel.render(entity.getFbxAnimation(tickDelta), seconds, CustomModelPose.HeadLookRotation.NONE, CustomModelPose.LimbPose.NONE);
        }
        if (vertices != null && !vertices.isEmpty()) {
            state.vertices = vertices;
        }
    }

    @Override
    public void submit(ViewEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (state.vertices.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, -state.yRot);
        for (Vertex vertex : state.vertices) {
            RenderType layer = RenderTypes.entityCutout(vertex.texture);
            submitNodeCollector.submitCustomGeometry(poseStack, layer, (entry, buffer) -> writeVertex(entry, buffer, vertex, state.lightCoords));
        }
        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    private static void writeVertex(PoseStack.Pose entry, VertexConsumer buffer, Vertex vertex, int light) {
        Matrix4f matrix = entry.pose();
        buffer.addVertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                .setColor(vertex.color)
                .setUv(vertex.textureUV.u, vertex.textureUV.v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
    }
}
