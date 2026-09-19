package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.entities.EnderEchoCrystalEntity;
import com.unddefined.enderechoing.entities.EnderEchoingEyeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.unddefined.enderechoing.EnderEchoing.MODID;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EnderEchoCrystalEntity>> ENDER_ECHO_CRYSTAL_ENTITY =
            ENTITIES.register("ender_echo_crystal_entity", () -> EntityType.Builder
                    .<EnderEchoCrystalEntity>of(EnderEchoCrystalEntity::new, MobCategory.MISC).fireImmune()
                    .sized(0.8F, 0.8F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE)
                    .build("ender_echo_crystal_entity"));

    public static final DeferredHolder<EntityType<?>, EntityType<EnderEchoingEyeEntity>> ENDER_ECHOING_EYE_ENTITY =
            ENTITIES.register("ender_echoing_eye_entity", () -> EntityType.Builder
                    .<EnderEchoingEyeEntity>of(EnderEchoingEyeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(4)
                    .build("ender_echoing_eye_entity"));
}
