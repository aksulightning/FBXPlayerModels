package me.onethecrazy.util.render;

import me.onethecrazy.util.network.LanSession;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.model.animation.CustomModelPose;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;

public final class FirstPersonSelfModelRenderer {
    private static final int FULL_BRIGHT_LIGHT = 0xF000F0;

    private FirstPersonSelfModelRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(FirstPersonSelfModelRenderer::render);
    }

    public static boolean shouldRenderFor(LocalPlayer player) {
        return getRenderVertices(player, 0.0f) != null;
    }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        PoseStack matrices = new PoseStack();
        matrices.mulPose(event.getPoseStack());
        MultiBufferSource.BufferSource consumers = client.renderBuffers().bufferSource();
        if (!LanSession.isAllowed() || player == null || client.level == null) {
            return;
        }

        float tickDelta = client.getTimer().getGameTimeDeltaPartialTick(false);
        @Nullable List<Vertex> vertices = getRenderVertices(player, tickDelta);
        if (vertices == null) {
            return;
        }

        Vec3 playerPos = player.getPosition(tickDelta);
        Vec3 cameraPos = event.getCamera().getPosition();
        int light = "Sit".equals(currentAnimation(player)) || client.level == null
                ? FULL_BRIGHT_LIGHT
                : LevelRenderer.getLightColor(client.level, BlockPos.containing(playerPos));

        matrices.pushPose();
        matrices.translate(playerPos.x - cameraPos.x, playerPos.y - cameraPos.y, playerPos.z - cameraPos.z);
        matrices.mulPose(Axis.YP.rotationDegrees(-event.getCamera().getYRot()));

        PoseStack.Pose entry = matrices.last();
        Matrix4f matrix = entry.pose();
        for (Vertex vertex : vertices) {
            RenderType layer = RenderType.entityCutoutNoCull(vertex.texture);
            VertexConsumer buffer = consumers.getBuffer(layer);
            buffer.addVertex(matrix, vertex.position.x, vertex.position.y, vertex.position.z)
                    .setColor(vertex.color)
                    .setUv(vertex.textureUV.u, vertex.textureUV.v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(entry, vertex.normals.x, vertex.normals.y, vertex.normals.z);
        }
        matrices.popPose();
        // This stage follows vanilla entity submission; finish the body before hand rendering.
        vertices.stream().map(vertex -> RenderType.entityCutoutNoCull(vertex.texture))
                .distinct().forEach(consumers::endBatch);
    }

    private static @Nullable List<Vertex> getRenderVertices(LocalPlayer player, float tickDelta) {
        if (!isRenderAllowed(player)) {
            return null;
        }

        CacheSkin cacheSkin = SkinManager.getSelfSkin();
        if (cacheSkin == null) {
            return null;
        }

        @Nullable List<Vertex> vertices = cacheSkin.vertices;
        if (cacheSkin.skinnedModel != null) {
            String animation = currentAnimation(player);
            float seconds = (player.tickCount + tickDelta) / 20f;
            boolean idle = "Idle".equals(animation);
            CustomModelPose.HeadLookRotation headLookRotation = idle
                    ? CustomModelPose.computeHeadLookRotation(player, tickDelta)
                    : CustomModelPose.HeadLookRotation.NONE;
            CustomModelPose.LimbPose limbPose = computeMinecraftLimbPose(player, tickDelta, animation);
            vertices = cacheSkin.skinnedModel.renderWithHiddenHead(animation, seconds, headLookRotation, limbPose, VoiceShapeClient.poseFor(cacheSkin.skinnedModel));
        }

        return vertices == null || vertices.isEmpty() ? null : vertices;
    }

    private static boolean isRenderAllowed(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        return LanSession.isAllowed() && FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()
                && FBXPlayerModelsClient.options().renderSelfModelInFirstPerson
                && client.getCameraEntity() == player
                && client.options.getCameraType() == CameraType.FIRST_PERSON
                && !player.isSpectator()
                && !player.isInvisible()
                && !player.hasPose(Pose.SLEEPING);
    }

    private static boolean isWalking(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    private static String currentAnimation(LocalPlayer player) {
        if (player.isPassenger()) {
            return "Sit";
        }
        if (player.isShiftKeyDown() || player.isCrouching()) {
            return "Sneak";
        }
        return isWalking(player) ? "Walk" : "Idle";
    }

    private static CustomModelPose.LimbPose computeMinecraftLimbPose(LocalPlayer player, float tickDelta, String animation) {
        if ("Sit".equals(animation)) {
            return sittingLimbPose();
        }
        if (!"Walk".equals(animation) && !"Sneak".equals(animation)) {
            return handActionPose(player, tickDelta);
        }

        boolean sneaking = "Sneak".equals(animation);
        float limbProgress = player.walkAnimation.position(tickDelta);
        float limbAmplitude = player.walkAnimation.speed(tickDelta);
        float armAmplitude = limbAmplitude;
        float legAmplitude = 1.4f * limbAmplitude;
        float sneakArmPitch = sneaking ? 0.4f : 0f;

        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f + Mth.PI) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f) * legAmplitude, 0.005f, 0.005f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f + Mth.PI) * legAmplitude, -0.005f, -0.005f)
        ).withArmAction(handActionPose(player, tickDelta));
    }

    private static CustomModelPose.LimbPose sittingLimbPose() {
        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(-0.62831855f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-0.62831855f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-1.5707964f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-1.5707964f, 0f, 0f)
        );
    }

    private static CustomModelPose.LimbPose handActionPose(LocalPlayer player, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        boolean breakingBlock = client.gameMode != null && client.gameMode.isDestroying();
        if (breakingBlock) {
            float phase = ((player.tickCount + tickDelta) % 8f) / 8f;
            InteractionHand hand = player.swinging ? player.swingingArm : InteractionHand.MAIN_HAND;
            return singleArmPose(player, hand, breakingRotation(phase));
        }

        float swing = player.getAttackAnim(tickDelta);
        if (swing <= 0f) {
            return CustomModelPose.LimbPose.NONE;
        }

        return singleArmPose(player, player.swingingArm, placingRotation(swing));
    }

    private static CustomModelPose.LimbPose singleArmPose(LocalPlayer player, InteractionHand hand, CustomModelPose.BodyPartRotation rotation) {
        boolean rightArm = isRightArm(player, hand);
        return rightArm
                ? CustomModelPose.LimbPose.NONE.withRightArm(rotation)
                : CustomModelPose.LimbPose.NONE.withLeftArm(rotation);
    }

    private static boolean isRightArm(LocalPlayer player, InteractionHand hand) {
        boolean mainArmRight = player.getMainArm() == HumanoidArm.RIGHT;
        return hand == InteractionHand.MAIN_HAND == mainArmRight;
    }

    private static CustomModelPose.BodyPartRotation breakingRotation(float phase) {
        float chop = Mth.sin(phase * Mth.TWO_PI);
        return new CustomModelPose.BodyPartRotation(-1.15f - 0.55f * chop, 0.18f * chop, 0.12f * chop);
    }

    private static CustomModelPose.BodyPartRotation placingRotation(float swing) {
        float ease = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        return new CustomModelPose.BodyPartRotation(-0.45f - 0.85f * ease, 0f, 0.18f * ease);
    }

}
