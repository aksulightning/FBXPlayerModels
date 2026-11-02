package com.aksulightning.platform.forge;

import com.aksulightning.platform.PlatformConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ForgePlatformConfig implements PlatformConfig {
    @Override
    public Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }
}
