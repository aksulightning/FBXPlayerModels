package com.aksulightning.fbxplayermodels;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ViewEntity extends Entity implements FbxModelEntity {
    public static final String MODEL_NBT_KEY = "Model";
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(ViewEntity.class, EntityDataSerializers.STRING);

    public ViewEntity(EntityType<? extends ViewEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public String getModel() {
        return getEntityData().get(MODEL);
    }

    public void setModel(String model) {
        getEntityData().set(MODEL, ViewEntityModelPath.safeModelOrEmpty(model));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODEL, "");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setModel(input.getStringOr(MODEL_NBT_KEY, ""));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        String model = getModel();
        if (!model.isBlank()) {
            output.putString(MODEL_NBT_KEY, model);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }
}
