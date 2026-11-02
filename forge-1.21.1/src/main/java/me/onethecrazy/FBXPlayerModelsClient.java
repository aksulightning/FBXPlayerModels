package me.onethecrazy;

import com.aksulightning.platform.PlatformServices;
import com.aksulightning.platform.forge.ForgePlatformClient;
import com.aksulightning.platform.forge.ForgePlatformConfig;
import com.aksulightning.platform.forge.ForgePlatformEvents;
import com.aksulightning.platform.forge.ForgePlatformLogger;
import me.onethecrazy.commands.Commands;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.objects.save.FBXPlayerModelsSave;
import me.onethecrazy.util.render.FirstPersonSelfModelRenderer;
import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.util.render.TitleScreenPreview;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class FBXPlayerModelsClient {
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
        return options;
    }

    public static void initialize(FMLJavaModLoadingContext context) {
        PlatformServices.initialize(
                new ForgePlatformLogger(FBXPlayerModelsMod.LOGGER),
                new ForgePlatformConfig(),
                new ForgePlatformClient(),
                new ForgePlatformEvents()
        );
        FBXPlayerModelsMod.LOGGER.info("Initializing {}", FBXPlayerModels.DISPLAY_NAME);
        FileUtil.createPaths();
        me.onethecrazy.util.network.LanSession.initialize();
        MinecraftForge.EVENT_BUS.addListener(Commands::register);
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ConfigScreen::create));
        PlatformServices.events().registerClientStarted(SkinManager::loadSelfSkin);
        FirstPersonSelfModelRenderer.register();
        TitleScreenPreview.register();
    }
}
