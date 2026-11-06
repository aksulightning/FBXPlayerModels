package com.aksulightning.fbxplayermodels.forge1122;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = FBXPlayerModelsMod.ID, name = "FBX Player Models (Client Only)",
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*",
        dependencies = "required-after:forge@[14.23.5.2847,)", clientSideOnly = true,
        useMetadata = true, guiFactory = "com.aksulightning.fbxplayermodels.forge1122.client.ModelGuiFactory")
public final class FBXPlayerModelsMod {
    public static final String ID = "fbxplayermodels";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        ClientModels.initialize();
    }
}
