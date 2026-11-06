package com.aksulightning.fbxplayermodels.forge1122.util.render;

import com.aksulightning.fbxplayermodels.forge1122.util.model.animation.CustomModelPose;
import com.aksulightning.fbxplayermodels.forge1122.ClientModels;
import com.aksulightning.fbxplayermodels.forge1122.util.objects.Vertex;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public final class PlayerPose {
    private PlayerPose() {
    }

    public static List<Vertex> vertices(EntityPlayer player, float tickDelta, boolean hideHead) {
        List<Vertex> vertices = ClientModels.staticVertices();
        if (ClientModels.model() == null) {
            return vertices;
        }

        String animation = currentAnimation(player);
        float seconds = (player.ticksExisted + tickDelta) / 20f;
        boolean sleeping = "Sleep".equals(animation);
        CustomModelPose.HeadLookRotation headLook = sleeping
                ? CustomModelPose.HeadLookRotation.NONE
                : CustomModelPose.computeHeadLookRotation(player, tickDelta);
        CustomModelPose.LimbPose limbs = limbPose(player, tickDelta, animation);
        if (hideHead) {
            return ClientModels.model().renderWithHiddenHead(animation, seconds, headLook, limbs);
        }
        // Player gaze is independent of gait/posture. The normal model render
        // applies look only to Idle, so use its existing overlay while awake;
        // otherwise Walk/Sneak/Sit silently freeze the head at the bind pose.
        return sleeping
                ? ClientModels.model().render(animation, seconds, headLook, limbs)
                : ClientModels.model().renderWithForcedHeadLook(animation, seconds, headLook, limbs);
    }

    public static String currentAnimation(EntityPlayer player) {
        if (player.isPlayerSleeping()) {
            return "Sleep";
        }
        if (player.isRiding()) {
            return "Sit";
        }
        if (player.isSneaking()) {
            return "Sneak";
        }
        return isWalking(player) ? "Walk" : "Idle";
    }

    public static float interpolatedBodyYaw(EntityPlayer player, float tickDelta) {
        return CustomModelPose.lerpAngle(tickDelta, player.prevRenderYawOffset, player.renderYawOffset);
    }

    public static float interpolatedViewYaw(EntityPlayer player, float tickDelta) {
        return CustomModelPose.lerpAngle(tickDelta, player.prevRotationYaw, player.rotationYaw);
    }

    private static boolean isWalking(EntityPlayer player) {
        if (player.limbSwingAmount > 0.01f) {
            return true;
        }
        double dx = player.posX - player.prevPosX;
        double dz = player.posZ - player.prevPosZ;
        return dx * dx + dz * dz > 0.0004
                || player.motionX * player.motionX + player.motionZ * player.motionZ > 0.0004;
    }

    private static CustomModelPose.LimbPose limbPose(EntityPlayer player, float tickDelta, String animation) {
        if ("Sit".equals(animation)) {
            return sittingPose();
        }

        CustomModelPose.LimbPose action = handActionPose(player, tickDelta);
        if (!"Walk".equals(animation) && !"Sneak".equals(animation)) {
            return action;
        }

        boolean sneaking = "Sneak".equals(animation);
        float progress = player.limbSwing;
        float amplitude = CustomModelPose.lerp(
                tickDelta,
                player.prevLimbSwingAmount,
                player.limbSwingAmount
        );

        // Zero native amplitude means the player is still, including while sneaking.
        float armAmplitude = amplitude;
        float legAmplitude = 1.4f * amplitude;
        float sneakArmPitch = sneaking ? 0.4f : 0f;
        CustomModelPose.LimbPose walk = new CustomModelPose.LimbPose(
                rotation(MathHelper.cos(progress * 0.6662f + (float) Math.PI) * armAmplitude + sneakArmPitch),
                rotation(MathHelper.cos(progress * 0.6662f) * armAmplitude + sneakArmPitch),
                new CustomModelPose.BodyPartRotation(MathHelper.cos(progress * 0.6662f) * legAmplitude, 0.005f, 0.005f),
                new CustomModelPose.BodyPartRotation(MathHelper.cos(progress * 0.6662f + (float) Math.PI) * legAmplitude, -0.005f, -0.005f)
        );
        return walk.withArmAction(action);
    }

    private static CustomModelPose.BodyPartRotation rotation(float pitch) {
        return new CustomModelPose.BodyPartRotation(pitch, 0f, 0f);
    }

    private static CustomModelPose.LimbPose sittingPose() {
        return new CustomModelPose.LimbPose(
                rotation(-0.62831855f),
                rotation(-0.62831855f),
                rotation(-1.5707964f),
                rotation(-1.5707964f)
        );
    }

    private static CustomModelPose.LimbPose handActionPose(EntityPlayer player, float tickDelta) {
        CustomModelPose.BodyPartRotation main = player.getHeldItemMainhand().isEmpty()
                ? CustomModelPose.BodyPartRotation.NONE
                : rotation(-0.2f);
        CustomModelPose.BodyPartRotation off = player.getHeldItemOffhand().isEmpty()
                ? CustomModelPose.BodyPartRotation.NONE : rotation(-0.2f);
        boolean rightHanded = player.getPrimaryHand() == EnumHandSide.RIGHT;
        CustomModelPose.BodyPartRotation right = rightHanded ? main : off;
        CustomModelPose.BodyPartRotation left = rightHanded ? off : main;
        float swing = player.getSwingProgress(tickDelta);
        if (swing > 0f) {
            float ease = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
            CustomModelPose.BodyPartRotation action = new CustomModelPose.BodyPartRotation(
                    -0.45f - 0.85f * ease, 0f, 0.18f * ease);
            boolean rightSwing = (player.swingingHand != EnumHand.OFF_HAND) == rightHanded;
            if (rightSwing) right = right.add(action);
            else left = left.add(action);
        }
        return CustomModelPose.LimbPose.NONE.withRightArm(right).withLeftArm(left);
    }
}
