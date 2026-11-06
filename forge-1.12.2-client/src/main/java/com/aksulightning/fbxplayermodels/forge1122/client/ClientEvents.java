package com.aksulightning.fbxplayermodels.forge1122.client;

import com.aksulightning.fbxplayermodels.forge1122.ClientModels;
import com.aksulightning.fbxplayermodels.forge1122.util.objects.Vertex;
import com.aksulightning.fbxplayermodels.forge1122.util.render.LegacyModelRenderer;
import com.aksulightning.fbxplayermodels.forge1122.util.render.PlayerPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.network.NetworkManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumAction;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public final class ClientEvents {
    private static final int MENU_BUTTON = 32112;
    private NetworkManager observed;
    private boolean noticeShown;

    @SubscribeEvent
    public void menu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            event.getButtonList().add(new GuiButton(MENU_BUTTON, 4, event.getGui().height - 48,
                    100, 20, "FBX Models"));
        }
    }

    @SubscribeEvent
    public void menuClick(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu && event.getButton().id == MENU_BUTTON) {
            ClientModels.openSettings();
        }
    }

    public static boolean allowed() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return false;
        return mc.isIntegratedServerRunning() || mc.getConnection() != null
                && LanAddressPolicy.isLanAddress(mc.getConnection().getNetworkManager().getRemoteAddress());
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getMinecraft();
        return ClientModels.config.enabled && ClientModels.hasModel() && mc.player != null
                && !mc.player.isInvisible() && (mc.world == null || allowed());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (ClientModels.SETTINGS.isPressed()) ClientModels.openSettings();
        Minecraft mc = Minecraft.getMinecraft();
        NetworkManager current = mc.getConnection() == null ? null : mc.getConnection().getNetworkManager();
        if (observed != current) { observed = current; noticeShown = false; }
        if (current != null && mc.player != null && mc.world != null && !allowed() && !noticeShown) {
            noticeShown = true;
            mc.player.sendMessage(new TextComponentString(LanAddressPolicy.UNSUPPORTED_MESSAGE));
        }
    }

    @SubscribeEvent
    public void player(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!active() || event.getEntityPlayer() != mc.player) return;
        float tick = event.getPartialRenderTick();
        List<Vertex> vertices = PlayerPose.vertices(mc.player, tick, false);
        if (vertices == null || vertices.isEmpty()) return;
        GlStateManager.pushMatrix();
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        try {
            GlStateManager.translate(event.getX(), event.getY(), event.getZ());
            rotatePlayer(tick);
            float scale = ClientModels.config.scale;
            GlStateManager.scale(scale, scale, scale);
            int light = mc.player.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
            GlStateManager.color(1f, 1f, 1f, 1f);
            LegacyModelRenderer.render(vertices);
            renderHeldItems(event, tick);
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
            GlStateManager.popMatrix();
        }
        event.setCanceled(true);
    }

    private static void rotatePlayer(float tick) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player.isPlayerSleeping()) {
            GlStateManager.rotate(mc.player.getBedOrientationInDegrees(), 0f, 1f, 0f);
            GlStateManager.rotate(90f, 0f, 0f, 1f);
        } else {
            GlStateManager.rotate(-PlayerPose.interpolatedBodyYaw(mc.player, tick), 0f, 1f, 0f);
        }
    }

    private static void renderHeldItems(RenderPlayerEvent.Pre event, float tick) {
        // Keep the vanilla biped's invisible arm transforms for its held-item layer.
        // Custom geometry is authored in Y-up block space; biped parts use Y-down pixels.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player.getHeldItemMainhand().isEmpty() && mc.player.getHeldItemOffhand().isEmpty()) return;
        ModelPlayer model = event.getRenderer().getMainModel();
        model.isRiding = mc.player.isRiding();
        model.isChild = false;
        model.bipedRightArm.showModel = true;
        model.bipedLeftArm.showModel = true;
        model.isSneak = mc.player.isSneaking();
        ModelBiped.ArmPose main = armPose(mc.player.getHeldItemMainhand());
        ModelBiped.ArmPose off = armPose(mc.player.getHeldItemOffhand());
        model.rightArmPose = mc.player.getPrimaryHand() == EnumHandSide.RIGHT ? main : off;
        model.leftArmPose = mc.player.getPrimaryHand() == EnumHandSide.RIGHT ? off : main;
        model.swingProgress = mc.player.getSwingProgress(tick);
        model.setLivingAnimations(mc.player, mc.player.limbSwing, mc.player.limbSwingAmount, tick);
        model.setRotationAngles(mc.player.limbSwing, mc.player.limbSwingAmount, mc.player.ticksExisted + tick,
                0f, mc.player.rotationPitch, 0.0625f, mc.player);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.rotate(180f, 0f, 1f, 0f);
            GlStateManager.scale(-1f, -1f, 1f);
            GlStateManager.translate(0f, -1.501f, 0f);
            new LayerHeldItem(event.getRenderer()).doRenderLayer(mc.player, mc.player.limbSwing,
                    mc.player.limbSwingAmount, tick, mc.player.ticksExisted + tick, 0f,
                    mc.player.rotationPitch, 0.0625f);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static ModelBiped.ArmPose armPose(ItemStack item) {
        if (item.isEmpty()) return ModelBiped.ArmPose.EMPTY;
        if (Minecraft.getMinecraft().player.getActiveItemStack() == item) {
            if (item.getItemUseAction() == EnumAction.BLOCK) return ModelBiped.ArmPose.BLOCK;
            if (item.getItemUseAction() == EnumAction.BOW) return ModelBiped.ArmPose.BOW_AND_ARROW;
        }
        return ModelBiped.ArmPose.ITEM;
    }

    @SubscribeEvent
    public void firstPerson(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!active() || !ClientModels.config.firstPerson || ClientModels.model() == null
                || !ClientModels.model().canHideHead()
                || mc.gameSettings.thirdPersonView != 0 || mc.getRenderViewEntity() != mc.player
                || mc.player.isPlayerSleeping() || mc.player.isSpectator()) return;
        float tick = event.getPartialTicks();
        List<Vertex> vertices = PlayerPose.vertices(mc.player, tick, true);
        GlStateManager.pushMatrix();
        try {
            double x = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * tick;
            double y = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * tick;
            double z = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * tick;
            GlStateManager.translate(x - mc.getRenderManager().viewerPosX,
                    y - mc.getRenderManager().viewerPosY, z - mc.getRenderManager().viewerPosZ);
            rotatePlayer(tick);
            float scale = ClientModels.config.scale;
            GlStateManager.scale(scale, scale, scale);
            RenderHelper.enableStandardItemLighting();
            LegacyModelRenderer.render(vertices);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (active() && mc.gameSettings.thirdPersonView == 0 && event.getEntity() == mc.player) {
            GlStateManager.translate(-ClientModels.config.cameraX, -ClientModels.config.cameraY,
                    -ClientModels.config.cameraZ);
        }
    }
}
