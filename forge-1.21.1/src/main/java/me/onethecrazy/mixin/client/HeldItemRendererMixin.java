package me.onethecrazy.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.onethecrazy.util.render.FirstPersonSelfModelRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V", at = @At("HEAD"), cancellable = true)
    private void fbx_player_models$hideFirstPersonHands(float tickDelta, PoseStack matrices, MultiBufferSource.BufferSource vertexConsumers, LocalPlayer player, int light, CallbackInfo ci) {
        if (FirstPersonSelfModelRenderer.shouldRenderFor(player)) {
            ci.cancel();
        }
    }
}
