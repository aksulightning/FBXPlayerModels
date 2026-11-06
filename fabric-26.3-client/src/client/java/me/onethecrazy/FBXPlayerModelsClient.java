package me.onethecrazy;

import com.aksulightning.platform.PlatformServices;
import com.aksulightning.platform.fabric.FabricPlatformClient;
import com.aksulightning.platform.fabric.FabricPlatformConfig;
import com.aksulightning.platform.fabric.FabricPlatformEvents;
import com.aksulightning.platform.fabric.FabricPlatformLogger;
import me.onethecrazy.commands.Commands;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.objects.save.FBXPlayerModelsSave;
import me.onethecrazy.util.render.FirstPersonSelfModelRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
        registerCommands();
        PlatformServices.events().registerClientStarted(SkinManager::loadSelfSkin);
        PlatformServices.events().registerClientJoined(SkinManager::loadSelfSkin);
        FirstPersonSelfModelRenderer.register();
        me.onethecrazy.util.render.VoiceShapeClient.register();
    }

    private void registerCommands() {
        Commands.initializeCommands();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(Commands.SKINS_COMMAND));
    }
}
