package me.onethecrazy.screens.rendering;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SkinPreviewRenderer {
    private final int x, y;
    private final int dimensions;
    private final float scale;
    private float yaw, pitch = 0;
    private String selectedPreviewHash = "";
    @Nullable private CacheSkin selectedPreviewCache;

    public SkinPreviewRenderer(int x, int y, int dimensions, float scale){
        this.x = x;
        this.y = y;
        this.dimensions = dimensions;
        this.scale = scale;
    }

    public void renderPreview(DrawContext ctx, float deltaTicks){
        MinecraftClient client = MinecraftClient.getInstance();
        AbstractClientPlayerEntity player = client.player;

        // Reset Player to render the correct Skin
        resetPlayerOnLivingEntityRenderer(player);

        // Prefer the cached custom model path for previews. InventoryScreen rotates the
        // player entity for vanilla skins, which flips custom model previews backward.
        boolean renderedCustomPreview = renderCachedSelfSkin(ctx, client, deltaTicks);

        // Render the Preview of the player skin
        if (!renderedCustomPreview && player != null && shouldRenderVanillaPlayerFallback()) {
            InventoryScreen.drawEntity(
                ctx,
                x, y,
                x + dimensions, y + dimensions,
                Math.round(scale),
                0f,
                yaw,
                pitch,
                player
            );
        }

        // Render the border where the Mesh is placed inside
        ctx.drawBorder(x, y, dimensions, dimensions, 0xFFFFFFFF);
    }

    public void addRotation(float yaw, float pitch){
        this.yaw += yaw;
        this.pitch += pitch;
    }

    private void resetPlayerOnLivingEntityRenderer(AbstractClientPlayerEntity player){
        if (player == null) {
            return;
        }

        EntityRenderDispatcher disp = MinecraftClient.getInstance().getEntityRenderDispatcher();
        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer) disp.getRenderer(player);

        ((LivingEntityRenderExtension)playerRenderer).fbx_player_models$setPlayerAsNull();
    }

    private boolean renderCachedSelfSkin(DrawContext ctx, MinecraftClient client, float deltaTicks) {
        if (!FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()) {
            return false;
        }

        UUID selfUuid = client.getSession().getUuidOrNull();
        @Nullable CacheSkin cacheSkin = selfUuid == null ? null : SkinManager.skinCache.get(selfUuid.toString());
        if (!hasRenderablePreview(cacheSkin)) {
            cacheSkin = getSelectedPreviewCache();
        }

        if (!hasRenderablePreview(cacheSkin)) {
            return false;
        }

        List<Vertex> vertices = cacheSkin.vertices;
        if (cacheSkin.skinnedModel != null) {
            float seconds = (client.getRenderTime() + deltaTicks) / 1000f;
            vertices = cacheSkin.skinnedModel.render(
                    "Idle",
                    seconds,
                    CustomModelPose.HeadLookRotation.NONE,
                    CustomModelPose.LimbPose.NONE
            );
        }

        if (vertices == null || vertices.isEmpty()) {
            return false;
        }

        ctx.enableScissor(x, y, x + dimensions, y + dimensions);

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(x + dimensions / 2f, y + dimensions - 6f, 100f);
        matrices.scale(scale, -scale, scale);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        int light = LightmapTextureManager.pack(15, 15);

        for (Vertex vertex : vertices) {
            VertexConsumer buffer = ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(vertex.texture));
            buffer.vertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                    .color(vertex.color)
                    .texture(vertex.textureUV.u, vertex.textureUV.v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
        }

        ctx.draw();
        matrices.pop();
        ctx.disableScissor();
        return true;
    }

    private @Nullable CacheSkin getSelectedPreviewCache() {
        var selectedSkin = FBXPlayerModelsClient.options().selectedSkin;
        String selectedHash = selectedSkin == null ? "" : selectedSkin.hash;
        if (!Objects.equals(selectedPreviewHash, selectedHash)) {
            selectedPreviewHash = selectedHash;
            selectedPreviewCache = SkinManager.loadSelectedSkinPreview();
        }

        return selectedPreviewCache;
    }

    private boolean shouldRenderVanillaPlayerFallback() {
        var selectedSkin = FBXPlayerModelsClient.options().selectedSkin;
        return !FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()
                || selectedSkin == null
                || selectedSkin.hash == null
                || selectedSkin.hash.isBlank();
    }

    private boolean hasRenderablePreview(@Nullable CacheSkin cacheSkin) {
        return cacheSkin != null
                && (cacheSkin.skinnedModel != null || (cacheSkin.vertices != null && !cacheSkin.vertices.isEmpty()));
    }
}
