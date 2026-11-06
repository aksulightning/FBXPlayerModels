package me.onethecrazy;

import com.mojang.blaze3d.platform.InputConstants;
import com.aksulightning.platform.PlatformServices;
import com.aksulightning.platform.fabric.FabricPlatformClient;
import com.aksulightning.platform.fabric.FabricPlatformConfig;
import com.aksulightning.platform.fabric.FabricPlatformEvents;
import com.aksulightning.platform.fabric.FabricPlatformLogger;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.objects.save.FBXPlayerModelsSave;
import me.onethecrazy.util.render.FirstPersonSelfModelRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class FBXPlayerModelsClient implements ClientModInitializer {
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
        if (options.selectedSkin == null) {
            options.selectedSkin = new me.onethecrazy.util.objects.save.ClientSkin();
        }
        return options;
    }

    @Override
    public void onInitializeClient() {
        PlatformServices.initialize(
                new FabricPlatformLogger(FBXPlayerModelsMod.LOGGER),
                new FabricPlatformConfig(),
                new FabricPlatformClient(),
                new FabricPlatformEvents()
        );
        FBXPlayerModelsMod.LOGGER.info("Initializing {}", DISPLAY_NAME);
        FileUtil.createPaths();
        me.onethecrazy.util.network.LanSession.initialize();
        registerKeyBindings();
        PlatformServices.events().registerClientStarted(SkinManager::loadSelfSkin);
        PlatformServices.events().registerClientJoined(SkinManager::loadSelfSkin);
        FirstPersonSelfModelRenderer.register();
        me.onethecrazy.util.render.VoiceShapeClient.register();
    }

    private void registerKeyBindings() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(FBXPlayerModelsMod.MOD_ID, "main"));
        KeyMapping openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fbxplayermodels.open_menu", InputConstants.KEY_O, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(ConfigScreen.create(null));
                }
            }
        });
    }
}
