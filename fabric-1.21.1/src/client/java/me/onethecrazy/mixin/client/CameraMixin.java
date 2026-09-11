package me.onethecrazy.mixin.client;

import me.onethecrazy.FBXPlayerModelsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void moveBy(float x, float y, float z);

    @Inject(method = "update", at = @At("TAIL"))
    private void fbx_player_models$applyFirstPersonCameraOffset(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()
                || !FBXPlayerModelsClient.options().renderSelfModelInFirstPerson
                || thirdPerson
                || !(focusedEntity instanceof ClientPlayerEntity)
                || client.getCameraEntity() != focusedEntity) {
            return;
        }

        float x = FBXPlayerModelsClient.options().firstPersonCameraOffsetX;
        float y = FBXPlayerModelsClient.options().firstPersonCameraOffsetY;
        float z = FBXPlayerModelsClient.options().firstPersonCameraOffsetZ;
        moveBy(-z, y, x);
    }
}
