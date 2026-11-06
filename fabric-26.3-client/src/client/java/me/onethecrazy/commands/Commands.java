package me.onethecrazy.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.onethecrazy.screens.ConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;


public class Commands {
    public static LiteralArgumentBuilder<FabricClientCommandSource> SKINS_COMMAND;

    public static void initializeCommands(){
        SKINS_COMMAND = ClientCommands.literal("skin")
                        .executes(context -> waypointsCommandHandler());
    }

    private static int waypointsCommandHandler(){
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().gui.setScreen(ConfigScreen.create(null))
        );
        return 1;
    }
}
