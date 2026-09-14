package com.unddefined.enderechoing.server.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreativeModeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "enderechoing");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EnderEchoing = CREATIVE_MODE_TABS.register("enderechoing", () ->
        CreativeModeTab.builder()
            .title(Component.nullToEmpty("Ender Echoing"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ItemRegistry.ENDER_ECHOING_CORE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(Items.ECHO_SHARD);
                output.accept(Items.ENDER_PEARL);
                output.accept(ItemRegistry.ENDER_ECHOING_CORE.get());
                output.accept(ItemRegistry.WARP_CORE.get());
                output.accept(ItemRegistry.ENDER_ECHO_TUNE_CHAMBER.get());
                output.accept(ItemRegistry.ENDER_ECHO_CRYSTAL.get());
                output.accept(Items.RECOVERY_COMPASS);
                output.accept(ItemRegistry.ENDER_ECHO_COMPASS.get());
                output.accept(ItemRegistry.RHYME_SHARD.get());
                output.accept(ItemRegistry.ENDER_ECHOING_PEARL.get());
                output.accept(ItemRegistry.ENDER_ECHOING_EYE.get());
                output.accept(ItemRegistry.ECHO_DRUSE.get());
                output.accept(ItemRegistry.WHISPER_DRUSE.get());
                output.accept(ItemRegistry.SCULK_MATTER.get());
                output.accept(Items.SCULK);
                output.accept(Items.SCULK_VEIN);
                output.accept(Items.SCULK_CATALYST);
                output.accept(Items.SCULK_SENSOR);
                output.accept(Items.CALIBRATED_SCULK_SENSOR);
                output.accept(Items.SCULK_SHRIEKER);
                output.accept(ItemRegistry.CALIBRATED_SCULK_SHRIEKER_ITEM.get());
                output.accept(ItemRegistry.SCULK_WHISPER_ITEM.get());
                output.accept(ItemRegistry.ECHO_DRUSE_STAGE1_ITEM.get());
                output.accept(ItemRegistry.ECHO_DRUSE_STAGE2_ITEM.get());
                output.accept(ItemRegistry.ECHO_DRUSE_STAGE3_ITEM.get());
                output.accept(ItemRegistry.ECHO_DRUSE_STAGE4_ITEM.get());
                output.accept(PotionContents.createItemStack(Items.POTION, PotionRegistry.SCULK_INTRUSION));
                output.accept(PotionContents.createItemStack(Items.SPLASH_POTION, PotionRegistry.SCULK_INTRUSION));
                output.accept(PotionContents.createItemStack(Items.LINGERING_POTION, PotionRegistry.SCULK_INTRUSION));
                output.accept(ItemRegistry.SCULK_SPREADER_SPAWN_EGG.get());
                output.accept(ItemRegistry.SCULK_ZOMBIE_SPAWN_EGG.get());
                output.accept(ItemRegistry.SCULK_CREEPER_SPAWN_EGG.get());
                output.accept(ItemRegistry.SCULK_SKELETON_SPAWN_EGG.get());
                output.accept(Items.WARDEN_SPAWN_EGG);
            }).build());
}
