package me.onethecrazy;

import com.mojang.blaze3d.platform.InputConstants;
import com.aksulightning.platform.PlatformServices;
import com.aksulightning.platform.forge.ForgePlatformClient;
import com.aksulightning.platform.forge.ForgePlatformConfig;
import com.aksulightning.platform.forge.ForgePlatformEvents;
import com.aksulightning.platform.forge.ForgePlatformLogger;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.objects.save.FBXPlayerModelsSave;
import me.onethecrazy.util.render.FirstPersonSelfModelRenderer;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.util.render.TitleScreenPreview;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

public final class FBXPlayerModelsClient {
    public static final String DISPLAY_NAME = "FBX Player Models (Client Only)";
    @Nullable private static FBXPlayerModelsSave options;

    public static FBXPlayerModelsSave options() {
        try {
            if (options == null) {
                options = FileUtil.loadSave();
            }
        } catch (IOException exception) {
            FBXPlayerModelsMod.LOGGER.error("Error while loading the client configuration", exception);
        }

        if (options == null) {
            options = new FBXPlayerModelsSave();
        }
        if (options.selectedSkin == null) options.selectedSkin = new me.onethecrazy.util.objects.save.ClientSkin();
        return options;
    }

    public static void initialize(FMLJavaModLoadingContext context) {
        PlatformServices.initialize(
                new ForgePlatformLogger(FBXPlayerModelsMod.LOGGER),
                new ForgePlatformConfig(),
                new ForgePlatformClient(),
                new ForgePlatformEvents()
        );
        FBXPlayerModelsMod.LOGGER.info("Initializing {}", FBXPlayerModelsClient.DISPLAY_NAME);
        FileUtil.createPaths();
        me.onethecrazy.util.network.LanSession.initialize();
        registerKeyBindings(context);
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ConfigScreen::create));
        PlatformServices.events().registerClientStarted(SkinManager::loadSelfSkin);
        PlatformServices.events().registerClientJoined(SkinManager::loadSelfSkin);
        me.onethecrazy.util.render.VoiceShapeClient.register();
        FirstPersonSelfModelRenderer.register();
        TitleScreenPreview.register();
    }

    private static void registerKeyBindings(FMLJavaModLoadingContext context) {
        KeyMapping openMenu = new KeyMapping("key.fbxplayermodels.open_menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.category.fbx-player-models.main");
        context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> event.register(openMenu));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> {
            Minecraft client = Minecraft.getInstance();
            while (openMenu.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(ConfigScreen.create(null));
                }
            }
        });
    }
}
