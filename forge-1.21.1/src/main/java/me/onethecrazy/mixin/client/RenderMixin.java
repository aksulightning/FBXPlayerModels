package me.onethecrazy.mixin.client;

import me.onethecrazy.util.network.LanSession;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.FBXPlayerModelsMod;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.render.VoiceShapeClient;
import me.onethecrazy.util.LivingEntityRenderExtension;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderMixin <T extends LivingEntity> implements LivingEntityRenderExtension {
    @Unique private static final boolean fbx_player_models$debugHeadLook = Boolean.getBoolean("fbxplayermodels.debugHeadLook");
    @Unique private static boolean fbx_player_models$headLookDebugLogged;
    @Unique private AbstractClientPlayer player;

    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at=@At("HEAD"), cancellable = true)
    private void onPlayerRender(T livingEntity, float yaw, float tickDelta, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, CallbackInfo ci){
        // We only want to hook the player rendering
        if(livingEntity instanceof AbstractClientPlayer renderedPlayer){
            if (!LanSession.isRenderingAllowed() || !FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled() || renderedPlayer != Minecraft.getInstance().player)
                return;

            player = renderedPlayer;
            @Nullable CacheSkin cacheResult = SkinManager.getSelfSkin();

            // We don't have the skin data yet
            if(cacheResult == null)
                return;

            @Nullable List<Vertex> vertices = cacheResult.vertices;
            if(cacheResult.skinnedModel != null){
                String animation = fbx_player_models$currentAnimation(renderedPlayer, tickDelta);
                float seconds = (renderedPlayer.tickCount + tickDelta) / 20f;
                boolean idle = "Idle".equals(animation);
                CustomModelPose.HeadLookRotation headLookRotation = idle && player != null
                        ? CustomModelPose.computeHeadLookRotation(player, tickDelta)
                        : CustomModelPose.HeadLookRotation.NONE;
                CustomModelPose.LimbPose limbPose = fbx_player_models$computeMinecraftLimbPose(renderedPlayer, tickDelta, animation);
                fbx_player_models$logHeadLookDebug(animation, renderedPlayer, tickDelta, idle && player != null);
                vertices = cacheResult.skinnedModel.render(animation, seconds, headLookRotation, limbPose, VoiceShapeClient.poseFor(cacheResult.skinnedModel));
            }

            // User didn't select a skin
            if(vertices == null || vertices.isEmpty())
                return;

            matrixStack.pushPose();

            // --- Stolen from net.minecraft.client.render.entity.LivingEntityRenderer#render ---
            if (renderedPlayer.hasPose(Pose.SLEEPING)) {
                Direction direction = renderedPlayer.getBedOrientation();
                if (direction != null) {
                    float f = renderedPlayer.getEyeHeight() - 0.1F + 1.0F;
                    matrixStack.translate((float)(-direction.getStepX()) * f, 0.0F, (float)(-direction.getStepZ()) * f);
                }
            }

            // Render Nametag
            renderNameTagIfShouldRender(renderedPlayer, renderedPlayer.getDisplayName(), matrixStack, vertexConsumerProvider, light);

            // Apply body yaw only; custom HEAD look is applied around its own bind pivot during skinning.
            fbx_player_models$applyBodyTransform(renderedPlayer, matrixStack, tickDelta);

            // Get Matrices
            PoseStack.Pose entry = matrixStack.last();
            Matrix4f matrix = entry.pose();

            for(Vertex v : vertices){
                RenderType layer = RenderType.entityCutoutNoCull(v.texture);
                VertexConsumer buffer = vertexConsumerProvider.getBuffer(layer);

                buffer.addVertex(matrix, v.position.x, v.position.y, v.position.z).setColor(v.color).setUv(v.textureUV.u, v.textureUV.v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, v.normals.x, v.normals.y, v.normals.z);
            }

            matrixStack.popPose();

            ci.cancel();
        }
    }

    @Unique
    // Used to reset the rendered Player, since the flow is as following:
    // Update State (set player for UUID-getting) -> Render immediately -> Same process for other entity
    // To now reset the player we use this method.
    // This is used when rendering the Player Skin Preview.
    public void fbx_player_models$setPlayerAsNull(){
        player = null;
    }

    @Unique
    private boolean fbx_player_models$isWalking(AbstractClientPlayer renderedPlayer, float tickDelta) {
        float limbSpeed = renderedPlayer.walkAnimation.speed(tickDelta);
        if (limbSpeed > 0.01f) {
            return true;
        }

        if (fbx_player_models$horizontalMovementSquared(renderedPlayer) > 0.0004) {
            return true;
        }

        Vec3 velocity = renderedPlayer.getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004;
    }

    @Unique
    private double fbx_player_models$horizontalMovementSquared(AbstractClientPlayer renderedPlayer) {
        double dx = renderedPlayer.getX() - renderedPlayer.xo;
        double dz = renderedPlayer.getZ() - renderedPlayer.zo;
        return dx * dx + dz * dz;
    }

    @Unique
    private void fbx_player_models$applyBodyTransform(AbstractClientPlayer renderedPlayer, PoseStack matrixStack, float tickDelta) {
        if (renderedPlayer.hasPose(Pose.SLEEPING)) {
            Direction direction = renderedPlayer.getBedOrientation();
            float yaw = direction == null
                    ? Mth.rotLerp(tickDelta, renderedPlayer.yBodyRotO, renderedPlayer.yBodyRot)
                    : fbx_player_models$sleepDirectionToRotation(direction);
            matrixStack.mulPose(Axis.YP.rotationDegrees(yaw));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(90f));
            matrixStack.mulPose(Axis.YP.rotationDegrees(90f));
            return;
        }

        float bodyYaw = Mth.rotLerp(tickDelta, renderedPlayer.yBodyRotO, renderedPlayer.yBodyRot);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
    }

    @Unique
    private static float fbx_player_models$sleepDirectionToRotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> 90f;
            case WEST -> 0f;
            case NORTH -> 270f;
            case EAST -> 180f;
            default -> 0f;
        };
    }

    @Unique
    private String fbx_player_models$currentAnimation(AbstractClientPlayer renderedPlayer, float tickDelta) {
        if (renderedPlayer.hasPose(Pose.SLEEPING)) {
            return "Sleep";
        }
        if (renderedPlayer.isPassenger()) {
            return "Sit";
        }
        if (renderedPlayer.isShiftKeyDown() || renderedPlayer.isCrouching()) {
            return "Sneak";
        }
        return fbx_player_models$isWalking(renderedPlayer, tickDelta) ? "Walk" : "Idle";
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$computeMinecraftLimbPose(AbstractClientPlayer renderedPlayer, float tickDelta, String animation) {
        if ("Sit".equals(animation)) {
            return fbx_player_models$sittingLimbPose();
        }
        if (!"Walk".equals(animation) && !"Sneak".equals(animation)) {
            return fbx_player_models$handActionPose(renderedPlayer, tickDelta);
        }

        boolean sneaking = "Sneak".equals(animation);
        float limbProgress = renderedPlayer.walkAnimation.position(tickDelta);
        float limbAmplitude = renderedPlayer.walkAnimation.speed(tickDelta);
        if (limbAmplitude <= 0.01f && fbx_player_models$horizontalMovementSquared(renderedPlayer) > 0.0004) {
            limbProgress = (renderedPlayer.tickCount + tickDelta) * 0.9f;
            limbAmplitude = sneaking ? 0.45f : 1.0f;
        }
        float armAmplitude = limbAmplitude;
        float legAmplitude = 1.4f * limbAmplitude;
        float sneakArmPitch = sneaking ? 0.4f : 0f;

        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f + Mth.PI) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f) * armAmplitude + sneakArmPitch, 0f, 0f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f) * legAmplitude, 0.005f, 0.005f),
                new CustomModelPose.BodyPartRotation(Mth.cos(limbProgress * 0.6662f + Mth.PI) * legAmplitude, -0.005f, -0.005f)
        ).withArmAction(fbx_player_models$handActionPose(renderedPlayer, tickDelta));
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$sittingLimbPose() {
        return new CustomModelPose.LimbPose(
                new CustomModelPose.BodyPartRotation(-0.62831855f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-0.62831855f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-1.5707964f, 0f, 0f),
                new CustomModelPose.BodyPartRotation(-1.5707964f, 0f, 0f)
        );
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$handActionPose(AbstractClientPlayer renderedPlayer, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        boolean breakingBlock = renderedPlayer == client.player
                && client.gameMode != null
                && client.gameMode.isDestroying();

        if (breakingBlock) {
            float phase = ((renderedPlayer.tickCount + tickDelta) % 8f) / 8f;
            InteractionHand hand = renderedPlayer.swinging ? renderedPlayer.swingingArm : InteractionHand.MAIN_HAND;
            return fbx_player_models$singleArmPose(renderedPlayer, hand, fbx_player_models$breakingRotation(phase));
        }

        float swing = renderedPlayer.getAttackAnim(tickDelta);
        if (swing <= 0f) {
            return CustomModelPose.LimbPose.NONE;
        }

        return fbx_player_models$singleArmPose(renderedPlayer, renderedPlayer.swingingArm, fbx_player_models$placingRotation(swing));
    }

    @Unique
    private CustomModelPose.LimbPose fbx_player_models$singleArmPose(AbstractClientPlayer player, InteractionHand hand, CustomModelPose.BodyPartRotation rotation) {
        boolean rightArm = fbx_player_models$isRightArm(player, hand);
        return rightArm
                ? CustomModelPose.LimbPose.NONE.withRightArm(rotation)
                : CustomModelPose.LimbPose.NONE.withLeftArm(rotation);
    }

    @Unique
    private boolean fbx_player_models$isRightArm(AbstractClientPlayer player, InteractionHand hand) {
        boolean mainArmRight = player.getMainArm() == HumanoidArm.RIGHT;
        return hand == InteractionHand.MAIN_HAND == mainArmRight;
    }

    @Unique
    private CustomModelPose.BodyPartRotation fbx_player_models$breakingRotation(float phase) {
        float chop = Mth.sin(phase * Mth.TWO_PI);
        return new CustomModelPose.BodyPartRotation(-1.15f - 0.55f * chop, 0.18f * chop, 0.12f * chop);
    }

    @Unique
    private CustomModelPose.BodyPartRotation fbx_player_models$placingRotation(float swing) {
        float ease = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        return new CustomModelPose.BodyPartRotation(-0.45f - 0.85f * ease, 0f, 0.18f * ease);
    }

    @Unique
    private void fbx_player_models$logHeadLookDebug(String animation, AbstractClientPlayer renderedPlayer, float tickDelta, boolean usedPlayerLookRotation) {
        if (!fbx_player_models$debugHeadLook || fbx_player_models$headLookDebugLogged) {
            return;
        }

        float bodyYaw = Mth.rotLerp(tickDelta, renderedPlayer.yBodyRotO, renderedPlayer.yBodyRot);
        float headYaw = Mth.rotLerp(tickDelta, renderedPlayer.yHeadRotO, renderedPlayer.getYHeadRot());
        float relativeHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
        float pitch = Mth.lerp(tickDelta, renderedPlayer.xRotO, renderedPlayer.getXRot());

        FBXPlayerModelsMod.LOGGER.info(
                "Head look debug animation={} bodyYaw={} headYaw={} relativeHeadYaw={} pitch={} usedPlayerLookRotation={}",
                animation,
                bodyYaw,
                headYaw,
                relativeHeadYaw,
                pitch,
                usedPlayerLookRotation
        );
        fbx_player_models$headLookDebugLogged = true;
    }

    @Unique
    private void renderNameTagIfShouldRender(AbstractClientPlayer renderedPlayer, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light){
        if (renderedPlayer == Minecraft.getInstance().player) {
            return;
        }

        if (!renderedPlayer.shouldShowName()) {
            return;
        }

        boolean notSneaking = !renderedPlayer.isDiscrete();
        int yOffset = "deadmau5".equals(text.getString()) ? -10 : 0;
        matrices.pushPose();
        matrices.translate(0.0F, renderedPlayer.getBbHeight() + 0.5F, 0.0F);
        matrices.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        matrices.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrices.last().pose();
        Font textRenderer = Minecraft.getInstance().font;
        float x = (float)(-textRenderer.width((FormattedText)text)) / 2.0F;
        int backgroundColor = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        textRenderer.drawInBatch(text, x, (float)yOffset, -2130706433, false, matrix4f, vertexConsumers, notSneaking ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, backgroundColor, light);
        if (notSneaking) {
            textRenderer.drawInBatch(text, x, (float)yOffset, -1, false, matrix4f, vertexConsumers, Font.DisplayMode.NORMAL, 0, LightTexture.pack(15, 15));
        }

        matrices.popPose();
    }
}
