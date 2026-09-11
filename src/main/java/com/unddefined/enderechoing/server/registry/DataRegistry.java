package com.unddefined.enderechoing.server.registry;

import com.mojang.serialization.Codec;
import com.unddefined.enderechoing.server.DataComponents.EntityData;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.DataComponents.VisitedStructures;
import com.unddefined.enderechoing.server.SculkIntrusionSpreader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class DataRegistry {
    public static final EntityDataAccessor<Optional<UUID>> ENDER_EYE_OWNER = SynchedEntityData.defineId(EndCrystal.class, EntityDataSerializers.OPTIONAL_UUID);

    public static final DeferredRegister.DataComponents COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "enderechoing");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> POSITION = COMPONENT_TYPES.registerComponentType("position", builder -> builder.persistent(GlobalPos.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TBOUND = COMPONENT_TYPES.registerComponentType("teleporter_bound", builder -> builder.persistent(Codec.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EntityData>> ENTITY = COMPONENT_TYPES.registerComponentType("entity", builder -> builder.persistent(EntityData.CODEC));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "enderechoing");
    public static final Supplier<AttachmentType<MarkedPositionsManager>> MARKED_POSITIONS_CACHE = ATTACHMENT_TYPES.register(
            "marked_positions_cache", () -> AttachmentType.serializable(MarkedPositionsManager::new).copyOnDeath().build()
    );
    public static final Supplier<AttachmentType<Integer>> EE_PEARL_AMOUNT = ATTACHMENT_TYPES.register(
            "ee_pearl_amount", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build()
    );
    public static final Supplier<AttachmentType<Integer>> SELECTED_TUNER_TAB = ATTACHMENT_TYPES.register(
             "selected_tuner_tab", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build()
    );
    public static final Supplier<AttachmentType<List<ItemStack>>> ICON_LIST = ATTACHMENT_TYPES.register(
            "icon_list", () -> AttachmentType.builder(DataRegistry::initDefaultIconList).serialize(ItemStack.CODEC.listOf()).copyOnDeath().build()
    );
    public static final Supplier<AttachmentType<BlockPos>> EE_PEARL_POSITION = ATTACHMENT_TYPES.register(
            "ee_pearl_position", () -> AttachmentType.builder(() -> BlockPos.ZERO).build()
    );
    public static final Supplier<AttachmentType<VisitedStructures>> VISITED_STRUCTURES = ATTACHMENT_TYPES.register(
            "visited_structures", () -> AttachmentType.serializable(VisitedStructures::new).copyOnDeath().build()
    );
    public static final Supplier<AttachmentType<SculkIntrusionSpreader>> SCULK_SPREADER = ATTACHMENT_TYPES.register(
            "sculk_intrusion_spreader", () -> AttachmentType.builder(SculkIntrusionSpreader::new).build()
    );
    public static final Supplier<AttachmentType<Long>> SCULK_VEIL_START = ATTACHMENT_TYPES.register(
            "sculk_veil_start", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> SCULK_VEIL_LAST_TICK = ATTACHMENT_TYPES.register(
            "sculk_veil_last_tick", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> SCULK_VEIL_TOTAL = ATTACHMENT_TYPES.register(
            "sculk_veil_total", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> GLOWING_START = ATTACHMENT_TYPES.register(
            "glowing_start", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> GLOWING_LAST_TICK = ATTACHMENT_TYPES.register(
            "glowing_last_tick", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> GLOWING_TOTAL = ATTACHMENT_TYPES.register(
            "glowing_total", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build()
    );

    public static List<ItemStack> initDefaultIconList() {
        List<ItemStack> icons = new ArrayList<>();
        icons.add(new ItemStack(ItemRegistry.ENDER_ECHOING_PEARL.get()));
        icons.add(new ItemStack(Items.FILLED_MAP));
        icons.add(new ItemStack(Items.IRON_SWORD));
        icons.add(new ItemStack(Items.BELL));
        icons.add(new ItemStack(Items.IRON_PICKAXE));
        icons.add(new ItemStack(Items.LEATHER));
        icons.add(new ItemStack(Items.WATER_BUCKET));
        icons.add(new ItemStack(Items.REDSTONE_TORCH));
        icons.add(new ItemStack(Items.FURNACE));
        icons.add(new ItemStack(Items.PLAYER_HEAD));
        return icons;
    }
}
