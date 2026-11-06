package com.aksulightning.fbxplayermodels;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class FbxTameableEntity extends TamableAnimal implements FbxModelEntity {
    public static final String TAME_ITEM_NBT_KEY = "TameItem";
    public static final String TAME_ITEM_NBT_ALIAS = "tame_item";
    private static final String DEFAULT_TAME_ITEM = "minecraft:bone";
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(FbxTameableEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TAME_ITEM = SynchedEntityData.defineId(FbxTameableEntity.class, EntityDataSerializers.STRING);

    public FbxTameableEntity(EntityType<? extends FbxTameableEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createFbxTameableAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 6.0f, 2.0f));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 120, false));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODEL, "");
        builder.define(TAME_ITEM, DEFAULT_TAME_ITEM);
    }

    @Override
    public String getModel() {
        return getEntityData().get(MODEL);
    }

    public void setModel(String model) {
        getEntityData().set(MODEL, ViewEntityModelPath.safeModelOrEmpty(model));
    }

    public String getTameItemId() {
        return getEntityData().get(TAME_ITEM);
    }

    public void setTameItemId(String itemId) {
        getEntityData().set(TAME_ITEM, safeTameItemOrDefault(itemId));
    }

    @Override
    public String getFbxAnimation(float tickDelta) {
        Vec3 velocity = getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 0.0004 ? "Walk" : "Idle";
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame() && stack.is(getTameItem())) {
            if (!level().isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                tame(player);
                setOrderedToSit(false);
                setInSittingPose(false);
                level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player)) {
            if (!level().isClientSide()) {
                boolean sitting = !isOrderedToSit();
                setOrderedToSit(sitting);
                setInSittingPose(sitting);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setModel(input.getStringOr(ViewEntity.MODEL_NBT_KEY, ""));
        setTameItemId(input.getString(TAME_ITEM_NBT_KEY).or(() -> input.getString(TAME_ITEM_NBT_ALIAS)).orElse(DEFAULT_TAME_ITEM));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        String model = getModel();
        if (!model.isBlank()) {
            output.putString(ViewEntity.MODEL_NBT_KEY, model);
        }
        output.putString(TAME_ITEM_NBT_KEY, getTameItemId());
    }

    private Item getTameItem() {
        Identifier id = Identifier.tryParse(getTameItemId());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.BONE;
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    private static String safeTameItemOrDefault(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return DEFAULT_TAME_ITEM;
        }
        Identifier id = Identifier.tryParse(itemId.trim());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.ITEM.getValue(id) == Items.AIR) {
            return DEFAULT_TAME_ITEM;
        }
        return id.toString();
    }
}
