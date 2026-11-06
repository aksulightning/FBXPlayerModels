package com.aksulightning.fbxplayermodels;

import me.onethecrazy.FBXPlayerModels;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final Identifier VIEW_ENTITY_ID = Identifier.fromNamespaceAndPath(FBXPlayerModels.ENTITY_NAMESPACE, FBXPlayerModels.VIEW_ENTITY_PATH);
    private static final ResourceKey<EntityType<?>> VIEW_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, VIEW_ENTITY_ID);
    public static final Identifier PASSIVE_ENTITY_ID = Identifier.fromNamespaceAndPath(FBXPlayerModels.ENTITY_NAMESPACE, "passive_entity");
    public static final Identifier TAMEABLE_ENTITY_ID = Identifier.fromNamespaceAndPath(FBXPlayerModels.ENTITY_NAMESPACE, "tameable_entity");
    public static final Identifier NEUTRAL_ENTITY_ID = Identifier.fromNamespaceAndPath(FBXPlayerModels.ENTITY_NAMESPACE, "neutral_entity");
    public static final Identifier HOSTILE_ENTITY_ID = Identifier.fromNamespaceAndPath(FBXPlayerModels.ENTITY_NAMESPACE, "hostile_entity");

    public static final EntityType<ViewEntity> VIEW_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            VIEW_ENTITY_KEY,
            EntityType.Builder.of(ViewEntity::new, MobCategory.MISC)
                    .sized(0.6f, 2.0f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(VIEW_ENTITY_KEY)
    );

    public static final EntityType<FbxPassiveEntity> PASSIVE_ENTITY = registerMob(
            PASSIVE_ENTITY_ID,
            EntityType.Builder.of(FbxPassiveEntity::new, MobCategory.CREATURE)
    );

    public static final EntityType<FbxTameableEntity> TAMEABLE_ENTITY = registerMob(
            TAMEABLE_ENTITY_ID,
            EntityType.Builder.of(FbxTameableEntity::new, MobCategory.CREATURE)
    );

    public static final EntityType<FbxNeutralEntity> NEUTRAL_ENTITY = registerMob(
            NEUTRAL_ENTITY_ID,
            EntityType.Builder.of(FbxNeutralEntity::new, MobCategory.CREATURE)
    );

    public static final EntityType<FbxHostileEntity> HOSTILE_ENTITY = registerMob(
            HOSTILE_ENTITY_ID,
            EntityType.Builder.of(FbxHostileEntity::new, MobCategory.MONSTER)
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(PASSIVE_ENTITY, AbstractFbxPathfinderEntity.createFbxMobAttributes());
        FabricDefaultAttributeRegistry.register(TAMEABLE_ENTITY, FbxTameableEntity.createFbxTameableAttributes());
        FabricDefaultAttributeRegistry.register(NEUTRAL_ENTITY, AbstractFbxPathfinderEntity.createFbxMobAttributes());
        FabricDefaultAttributeRegistry.register(HOSTILE_ENTITY, FbxHostileEntity.createFbxHostileAttributes());
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registerMob(Identifier id, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                key,
                builder.sized(0.6f, 2.0f)
                        .eyeHeight(1.62f)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .build(key)
        );
    }
}
