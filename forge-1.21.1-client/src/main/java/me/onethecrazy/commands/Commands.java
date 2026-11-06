package me.onethecrazy.commands;

import me.onethecrazy.screens.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class Commands {
    private Commands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("skin")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> client.setScreen(ConfigScreen.create(null)));
                    return 1;
                }));
    }
}
