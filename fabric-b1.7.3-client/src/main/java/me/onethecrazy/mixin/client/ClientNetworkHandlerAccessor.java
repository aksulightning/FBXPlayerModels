package me.onethecrazy.mixin.client;

import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientNetworkHandler.class)
public interface ClientNetworkHandlerAccessor {
    @Accessor("connection")
    Connection fbx_player_models$getConnection();
}
