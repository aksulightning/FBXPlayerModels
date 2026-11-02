package me.onethecrazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(FBXPlayerModelsMod.MOD_ID)
public final class FBXPlayerModelsMod {
    public static final String MOD_ID = "fbx_player_models_lite";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public FBXPlayerModelsMod(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FBXPlayerModelsClient.initialize(context);
        }
    }
}
