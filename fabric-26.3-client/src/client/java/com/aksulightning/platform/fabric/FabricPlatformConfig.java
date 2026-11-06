package com.aksulightning.platform.fabric;

import com.aksulightning.platform.PlatformConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatformConfig implements PlatformConfig {
    @Override
    public Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }
}
