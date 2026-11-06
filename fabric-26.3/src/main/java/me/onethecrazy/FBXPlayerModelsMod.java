package me.onethecrazy;

import net.fabricmc.api.ModInitializer;
import com.aksulightning.fbxplayermodels.ModEntities;
import me.onethecrazy.server.ServerModelNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FBXPlayerModelsMod implements ModInitializer {
	public static final String MOD_ID = FBXPlayerModels.MOD_ID;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing " + MOD_ID);
		ModEntities.register();
		ServerModelNetworking.initialize();
	}
}
