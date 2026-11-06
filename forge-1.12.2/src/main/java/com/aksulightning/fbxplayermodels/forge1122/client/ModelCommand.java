package com.aksulightning.fbxplayermodels.forge1122.client;

import com.aksulightning.fbxplayermodels.forge1122.ClientModels;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Registered exclusively with ClientCommandHandler; these commands never reach a server. */
public final class ModelCommand extends CommandBase {
    @Override public String getName() { return "skin"; }
    @Override public String getUsage(ICommandSender sender) { return "/skin [import <path>|reset|toggle|reload]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return true; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            Minecraft.getMinecraft().addScheduledTask(ClientModels::openSettings);
            return;
        }
        switch (args[0]) {
            case "import":
                if (args.length < 2) { sender.sendMessage(new TextComponentString(getUsage(sender))); return; }
                String path = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (path.startsWith("\"") && path.endsWith("\"")) path = path.substring(1, path.length() - 1);
                try { ClientModels.select(Paths.get(path)); }
                catch (RuntimeException exception) { ClientModels.status = "Invalid model path: " + exception.getMessage(); }
                break;
            case "reset": ClientModels.reset(); break;
            case "reload": ClientModels.reload(); break;
            case "toggle":
                ClientModels.config.enabled = !ClientModels.config.enabled;
                ClientModels.config.save();
                ClientModels.status = "FBX models " + (ClientModels.config.enabled ? "enabled." : "disabled.");
                break;
            default: sender.sendMessage(new TextComponentString(getUsage(sender))); return;
        }
        sender.sendMessage(new TextComponentString(ClientModels.status));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "import", "reset", "toggle", "reload")
                : Collections.emptyList();
    }
}
