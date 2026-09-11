package me.onethecrazy.mixin.client;

import me.onethecrazy.FBXPlayerModelsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void move(float x, float y, float z);
    @Shadow public abstract Entity entity();

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void fbx_player_models$applyFirstPersonCameraOffset(DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity focusedEntity = entity();
        if (!FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()
                || !FBXPlayerModelsClient.options().renderSelfModelInFirstPerson
                || minecraft.options.getCameraType() != CameraType.FIRST_PERSON
                || !(focusedEntity instanceof LocalPlayer)
                || minecraft.getCameraEntity() != focusedEntity) {
            return;
        }

        float x = FBXPlayerModelsClient.options().firstPersonCameraOffsetX;
        float y = FBXPlayerModelsClient.options().firstPersonCameraOffsetY;
        float z = FBXPlayerModelsClient.options().firstPersonCameraOffsetZ;
        move(-z, y, x);
    }
}
