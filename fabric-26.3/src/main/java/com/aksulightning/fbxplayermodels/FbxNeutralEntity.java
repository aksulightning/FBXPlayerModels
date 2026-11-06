package com.aksulightning.fbxplayermodels;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;

public class FbxNeutralEntity extends AbstractFbxPathfinderEntity {
    public FbxNeutralEntity(EntityType<? extends FbxNeutralEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
}
