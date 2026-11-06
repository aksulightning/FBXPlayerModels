package me.onethecrazy.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.onethecrazy.util.render.FirstPersonSelfModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
    @Inject(method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void fbx_player_models$hideFirstPersonHands(float tickDelta, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState handsState, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (playerState.hasPlayer && player != null && FirstPersonSelfModelRenderer.shouldRenderFor(player)) {
            ci.cancel();
        }
    }
}
