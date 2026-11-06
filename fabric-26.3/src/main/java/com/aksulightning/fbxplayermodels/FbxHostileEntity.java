package com.aksulightning.fbxplayermodels;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class FbxHostileEntity extends Monster implements FbxModelEntity {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(FbxHostileEntity.class, EntityDataSerializers.STRING);

    public FbxHostileEntity(EntityType<? extends FbxHostileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createFbxHostileAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 120, false));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            return 10.0f;
        }
        return level.getPathfindingCostFromLightLevels(pos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODEL, "");
    }

    @Override
    public String getModel() {
        return getEntityData().get(MODEL);
    }

    public void setModel(String model) {
        getEntityData().set(MODEL, ViewEntityModelPath.safeModelOrEmpty(model));
    }

    @Override
    public String getFbxAnimation(float tickDelta) {
        Vec3 velocity = getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004 ? "Walk" : "Idle";
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setModel(input.getStringOr(ViewEntity.MODEL_NBT_KEY, ""));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        String model = getModel();
        if (!model.isBlank()) {
            output.putString(ViewEntity.MODEL_NBT_KEY, model);
        }
    }
}
