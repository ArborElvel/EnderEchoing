package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.blocks.EchoDruseBlock;
import com.unddefined.enderechoing.items.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("enderechoing");
    public static final DeferredItem<Item> ENDER_ECHOING_CORE = ITEMS.registerItem("ender_echoing_core", EnderEchoingCore::new);
    public static final DeferredItem<Item> ENDER_ECHO_TUNE_CHAMBER = ITEMS.registerItem("ender_echo_tune_chamber", EnderEchoTuneChamber::new);
    public static final DeferredItem<Item> ENDER_ECHO_CRYSTAL = ITEMS.registerItem("ender_echo_crystal", EnderEchoCrystal::new);
    public static final DeferredItem<Item> ENDER_ECHO_COMPASS = ITEMS.registerItem("ender_echo_compass", EnderEchoCompass::new);
    public static final DeferredItem<Item> ECHO_DRUSE = ITEMS.registerItem("echo_druse", EchoDruse::new);
    public static final DeferredItem<Item> WHISPER_DRUSE = ITEMS.registerItem("whisper_druse", WhisperDruse::new);
    public static final DeferredItem<Item> ENDER_ECHOING_PEARL = ITEMS.registerItem("ender_echoing_pearl", EnderEchoingPearl::new);
    public static final DeferredItem<Item> ENDER_ECHOING_EYE = ITEMS.registerItem("ender_echoing_eye", EnderEchoingEye::new);
    public static final DeferredItem<SpawnEggItem> SCULK_SPREADER_SPAWN_EGG = ITEMS.registerItem("sculk_spreader_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.SCULK_SPREADER_ENTITY, 0xFFFFFF, 0xFFFFFF, props));
    public static final DeferredItem<SpawnEggItem> SCULK_ZOMBIE_SPAWN_EGG = ITEMS.registerItem("sculk_zombie_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.SCULK_ZOMBIE_ENTITY, 0xFFFFFF, 0xFFFFFF, props));
    public static final DeferredItem<SpawnEggItem> CREESPER_SPAWN_EGG = ITEMS.registerItem("creesper_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.CREESPER_ENTITY, 0xFFFFFF, 0xFFFFFF, props));
    public static final DeferredItem<SpawnEggItem> SCULK_SKELETON_SPAWN_EGG = ITEMS.registerItem("sculk_skeleton_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.SCULK_SKELETON_ENTITY, 0xFFFFFF, 0xFFFFFF, props));
    public static final DeferredItem<Item> WARP_CORE = ITEMS.registerItem("warp_core", WarpCore::new);
    public static final DeferredItem<Item> SCULK_MATTER = ITEMS.registerSimpleItem("sculk_matter", new Item.Properties());
    public static final DeferredItem<Item> RHYME_SHARD = ITEMS.registerSimpleItem("rhyme_shard", new Item.Properties()
            .rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredItem<BlockItem> CALIBRATED_SCULK_SHRIEKER_ITEM = ITEMS.registerSimpleBlockItem("calibrated_sculk_shrieker", BlockRegistry.CALIBRATED_SCULK_SHRIEKER);
    public static final DeferredItem<BlockItem> SCULK_WHISPER_ITEM = ITEMS.registerSimpleBlockItem("sculk_whisper", BlockRegistry.SCULK_WHISPER);
    //region ECHO_DRUSE_BLOCKITEM register
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE1_ITEM = ITEMS.register("echo_druse_stage1", () -> new BlockItem(BlockRegistry.ECHO_DRUSE.get(), new Item.Properties()) {
        @Override
        public BlockState getPlacementState(BlockPlaceContext context) {
            return BlockRegistry.ECHO_DRUSE.get().defaultBlockState().setValue(EchoDruseBlock.GROWTH_STAGE, 1);
        }
    });
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE2_ITEM = ITEMS.register("echo_druse_stage2", () -> new BlockItem(BlockRegistry.ECHO_DRUSE.get(), new Item.Properties()) {
        @Override
        public BlockState getPlacementState(BlockPlaceContext context) {
            return BlockRegistry.ECHO_DRUSE.get().defaultBlockState().setValue(EchoDruseBlock.GROWTH_STAGE, 2);
        }
    });
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE3_ITEM = ITEMS.register("echo_druse_stage3", () -> new BlockItem(BlockRegistry.ECHO_DRUSE.get(), new Item.Properties()) {
        @Override
        public BlockState getPlacementState(BlockPlaceContext context) {
            return BlockRegistry.ECHO_DRUSE.get().defaultBlockState().setValue(EchoDruseBlock.GROWTH_STAGE, 3);
        }
    });
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE4_ITEM = ITEMS.register("echo_druse_stage4", () -> new BlockItem(BlockRegistry.ECHO_DRUSE.get(), new Item.Properties()) {
        @Override
        public BlockState getPlacementState(BlockPlaceContext context) {
            return BlockRegistry.ECHO_DRUSE.get().defaultBlockState().setValue(EchoDruseBlock.GROWTH_STAGE, 4);
        }
    });
    //endregion

}
