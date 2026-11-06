package me.onethecrazy.mixin.client;

import net.minecraft.client.render.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Tessellator.class)
public interface TessellatorAccessor {
    @Accessor("normal")
    void fbx_player_models$setPackedNormal(int normal);
}
