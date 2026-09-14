package me.onethecrazy.screens.rendering;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.render.CustomSkinRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SkinPreviewRenderer {
    private static final int FULL_BRIGHT_LIGHT = 0xF000F0;
    private static final float GUI_PREVIEW_BODY_ROTATION = 180f;
    private final int x, y;
    private final int dimensions;
    private final float scale;
    private float yaw, pitch = 0;
    private float zoom = 1f;
    private String selectedPreviewHash = "";
    @Nullable private CacheSkin selectedPreviewCache;

    public SkinPreviewRenderer(int x, int y, int dimensions, float scale){
        this.x = x;
        this.y = y;
        this.dimensions = dimensions;
        this.scale = scale;
    }

    public void renderPreview(GuiGraphicsExtractor ctx, float deltaTicks){
        Minecraft client = Minecraft.getInstance();
        AbstractClientPlayer player = client.player;
        boolean renderedCustomPreview = renderCachedSelfSkin(ctx, client, deltaTicks, player);

        // Render the Preview of the player skin
        if (!renderedCustomPreview && player != null && shouldRenderVanillaPlayerFallback()) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
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
        ctx.outline(x, y, dimensions, dimensions, 0xFFFFFFFF);
    }

    public void addRotation(float yaw, float pitch){
        this.yaw += yaw;
        this.pitch += pitch;
    }

    public void setView(float yaw, float pitch, float zoom) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.zoom = zoom;
    }

    /** Render a supplied default-pose model independently of the player display setting. */
    public void renderModelPreview(GuiGraphicsExtractor ctx, @Nullable CacheSkin cache) {
        List<Vertex> vertices = cache == null ? null : cache.skinnedModel == null
                ? cache.vertices : cache.skinnedModel.staticVertices();
        renderVertices(ctx, vertices, Minecraft.getInstance().player);
        ctx.outline(x, y, dimensions, dimensions, 0xFFFFFFFF);
    }

    private boolean renderCachedSelfSkin(GuiGraphicsExtractor ctx, Minecraft client, float deltaTicks, @Nullable AbstractClientPlayer player) {
        if (!FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()) {
            return false;
        }

        UUID selfUuid = client.getUser().getProfileId();
        @Nullable CacheSkin cacheSkin = selfUuid == null ? null : SkinManager.skinCache.get(selfUuid.toString());
        if (!hasRenderablePreview(cacheSkin)) {
            cacheSkin = getSelectedPreviewCache();
        }

        if (!hasRenderablePreview(cacheSkin)) {
            return false;
        }

        List<Vertex> vertices = cacheSkin.vertices;
        if (cacheSkin.skinnedModel != null) {
            float seconds = (System.nanoTime() / 1_000_000_000f) + deltaTicks / 20f;
            vertices = cacheSkin.skinnedModel.render(
                    "Idle",
                    seconds,
                    CustomModelPose.HeadLookRotation.NONE,
                    CustomModelPose.LimbPose.NONE
            );
        }

        return renderVertices(ctx, vertices, player);
    }

    private boolean renderVertices(GuiGraphicsExtractor ctx, @Nullable List<Vertex> vertices, @Nullable AbstractClientPlayer player) {
        if (vertices == null || vertices.isEmpty()) {
            return false;
        }

        AvatarRenderState state = new AvatarRenderState();
        state.skin = player == null ? DefaultPlayerSkin.getDefaultSkin() : player.getSkin();
        state.pose = Pose.STANDING;
        state.scale = 1f;
        state.boundingBoxWidth = 1f;
        state.boundingBoxHeight = 2f;
        state.eyeHeight = 1.62f;
        state.lightCoords = FULL_BRIGHT_LIGHT;
        state.bodyRot = GUI_PREVIEW_BODY_ROTATION;
        state.setData(CustomSkinRenderData.KEY, new CustomSkinRenderData(vertices, GUI_PREVIEW_BODY_ROTATION, state.pose, null, state.eyeHeight, FULL_BRIGHT_LIGHT));

        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(pitch));
        ctx.enableScissor(x, y, x + dimensions, y + dimensions);
        ctx.entity(
                state,
                Math.max(1, Math.round(scale * zoom)),
                new Vector3f(0f, 1f, 0f),
                rotation,
                new Quaternionf(rotation).conjugate(),
                x, y,
                x + dimensions, y + dimensions
        );
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
