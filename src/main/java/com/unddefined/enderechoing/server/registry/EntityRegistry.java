package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.entities.EnderEchoCrystalEntity;
import com.unddefined.enderechoing.entities.EnderEchoingEyeEntity;
import com.unddefined.enderechoing.entities.SculkSpreaderEntity;
import com.unddefined.enderechoing.entities.SculkZombieEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.unddefined.enderechoing.EnderEchoing.MODID;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>,EntityType<EnderEchoCrystalEntity>> ENDER_ECHO_CRYSTAL_ENTITY = ENTITIES.register("ender_echo_crystal_entity", () ->
                    EntityType.Builder.<EnderEchoCrystalEntity>of(EnderEchoCrystalEntity::new, MobCategory.MISC).fireImmune()
                            .sized(0.8F, 0.8F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE).build("ender_echo_crystal_entity")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<EnderEchoingEyeEntity>> ENDER_ECHOING_EYE_ENTITY = ENTITIES.register("ender_echoing_eye_entity", () ->
            EntityType.Builder.<EnderEchoingEyeEntity>of(EnderEchoingEyeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(4).build("ender_echoing_eye_entity")
    );

    public static final DeferredHolder<EntityType<?>,EntityType<SculkSpreaderEntity>> SCULK_SPREADER_ENTITY = ENTITIES.register("sculk_spreader_entity", () ->
            EntityType.Builder.of(SculkSpreaderEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.4F).clientTrackingRange(16).updateInterval(2).build("sculk_spreader_entity")
    );

    public static final DeferredHolder<EntityType<?>,EntityType<SculkZombieEntity>> SCULK_ZOMBIE_ENTITY = ENTITIES.register("sculk_zombie_entity", () ->
            EntityType.Builder.of(SculkZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("sculk_zombie_entity")
    );

}
