package me.onethecrazy.screens.rendering;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Objects;

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

    public void renderPreview(GuiGraphics ctx, float deltaTicks){
        Minecraft client = Minecraft.getInstance();
        AbstractClientPlayer player = client.player;

        // Reset Player to render the correct Skin
        resetPlayerOnLivingEntityRenderer(player);

        // Prefer the cached custom model path for previews. InventoryScreen rotates the
        // player entity for vanilla skins, which flips custom model previews backward.
        boolean renderedCustomPreview = renderCachedSelfSkin(ctx, client, deltaTicks);

        // Render the Preview of the player skin
        if (!renderedCustomPreview && player != null && shouldRenderVanillaPlayerFallback()) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
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
        ctx.renderOutline(x, y, dimensions, dimensions, 0xFFFFFFFF);
    }

    public void addRotation(float yaw, float pitch){
        this.yaw += yaw;
        this.pitch += pitch;
    }

    private void resetPlayerOnLivingEntityRenderer(AbstractClientPlayer player){
        if (player == null) {
            return;
        }

        EntityRenderDispatcher disp = Minecraft.getInstance().getEntityRenderDispatcher();
        PlayerRenderer playerRenderer = (PlayerRenderer) disp.getRenderer(player);

        ((LivingEntityRenderExtension)playerRenderer).fbx_player_models$setPlayerAsNull();
    }

    private boolean renderCachedSelfSkin(GuiGraphics ctx, Minecraft client, float deltaTicks) {
        if (!FBXPlayerModelsClient.options().isEnabled) {
            return false;
        }

        @Nullable CacheSkin cacheSkin = SkinManager.getSelfSkin();
        if (!hasRenderablePreview(cacheSkin)) {
            cacheSkin = getSelectedPreviewCache();
        }

        if (!hasRenderablePreview(cacheSkin)) {
            return false;
        }

        List<Vertex> vertices = cacheSkin.vertices;
        if (cacheSkin.skinnedModel != null) {
            float seconds = (client.getFrameTimeNs() + deltaTicks) / 1000f;
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

        PoseStack matrices = ctx.pose();
        matrices.pushPose();
        matrices.translate(x + dimensions / 2f, y + dimensions - 6f, 100f);
        matrices.scale(scale, -scale, scale);
        matrices.mulPose(Axis.YP.rotationDegrees(yaw));
        matrices.mulPose(Axis.XP.rotationDegrees(pitch));

        PoseStack.Pose entry = matrices.last();
        Matrix4f matrix = entry.pose();
        int light = LightTexture.pack(15, 15);

        for (Vertex vertex : vertices) {
            VertexConsumer buffer = ctx.bufferSource().getBuffer(RenderType.entityCutoutNoCull(vertex.texture));
            buffer.addVertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                    .setColor(vertex.color)
                    .setUv(vertex.textureUV.u, vertex.textureUV.v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
        }

        ctx.flush();
        matrices.popPose();
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
        return !FBXPlayerModelsClient.options().isEnabled
                || selectedSkin == null
                || selectedSkin.hash == null
                || selectedSkin.hash.isBlank();
    }

    private boolean hasRenderablePreview(@Nullable CacheSkin cacheSkin) {
        return cacheSkin != null
                && (cacheSkin.skinnedModel != null || (cacheSkin.vertices != null && !cacheSkin.vertices.isEmpty()));
    }
}
