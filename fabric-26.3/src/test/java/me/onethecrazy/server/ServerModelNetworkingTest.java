package me.onethecrazy.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.permissions.PermissionSet;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServerModelNetworkingTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void joiningPlayerCommandTreeAcceptsServerlessInspection() throws ReflectiveOperationException {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ServerModelNetworking.registerCommands(dispatcher, Commands.CommandSelection.INTEGRATED);

        var command = dispatcher.getRoot().getChild("fbxplayermodels");
        assertNotNull(command);
        CommandSourceStack restricted = Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS);
        CommandSourceStack administrator = Commands.createCompilationContext(PermissionSet.ALL_PERMISSIONS);
        assertNull(restricted.getServer());
        assertFalse(command.canUse(restricted));
        assertTrue(command.canUse(administrator));

        var inspectorField = Commands.class.getDeclaredField("COMMAND_NODE_INSPECTOR");
        inspectorField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var inspector = (ClientboundCommandsPacket.NodeInspector<CommandSourceStack>) inspectorField.get(null);
        assertTrue(inspector.isRestricted(command));
        new ClientboundCommandsPacket(dispatcher.getRoot(), inspector);
    }

    @Test
    public void dedicatedServersDoNotRegisterUploadPermissionCommands() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        ServerModelNetworking.registerCommands(dispatcher, Commands.CommandSelection.DEDICATED);
        assertNull(dispatcher.getRoot().getChild("fbxplayermodels"));
    }
}
