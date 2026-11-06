package com.aksulightning.fbxplayermodels;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class FbxPassiveEntity extends AbstractFbxPathfinderEntity {
    public FbxPassiveEntity(EntityType<? extends FbxPassiveEntity> entityType, Level level) {
        super(entityType, level);
    }
}
