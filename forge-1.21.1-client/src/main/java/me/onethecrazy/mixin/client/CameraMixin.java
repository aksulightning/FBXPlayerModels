package me.onethecrazy.mixin.client;

import me.onethecrazy.util.network.LanSession;

import me.onethecrazy.FBXPlayerModelsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void move(float x, float y, float z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void fbx_player_models$applyFirstPersonCameraOffset(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!LanSession.isAllowed() || !FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()
                || !FBXPlayerModelsClient.options().renderSelfModelInFirstPerson
                || thirdPerson
                || !(focusedEntity instanceof LocalPlayer)
                || client.getCameraEntity() != focusedEntity) {
            return;
        }

        float x = FBXPlayerModelsClient.options().firstPersonCameraOffsetX;
        float y = FBXPlayerModelsClient.options().firstPersonCameraOffsetY;
        float z = FBXPlayerModelsClient.options().firstPersonCameraOffsetZ;
        move(-z, y, x);
    }
}
