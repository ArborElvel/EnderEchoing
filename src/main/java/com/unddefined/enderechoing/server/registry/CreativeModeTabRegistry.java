package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.compat.sculkborne.CompatSculkRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreativeModeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "enderechoing");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENDER_ECHOING =
            CompatSculkRegistry.ACTIVE ? CREATIVE_MODE_TABS.register("enderechoing", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.enderechoing"))
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
                        output.accept(ItemRegistry.ENDER_ECHOING_PEARL.get());
                        output.accept(ItemRegistry.ENDER_ECHOING_EYE.get());
                        if (CompatSculkRegistry.ACTIVE) {
                            output.accept(CompatSculkRegistry.RHYME_SHARD.get());
                            output.accept(CompatSculkRegistry.ECHO_DRUSE.get());
                            output.accept(CompatSculkRegistry.ECHO_DRUSE_BLOCK_ITEM.get());
                            output.accept(CompatSculkRegistry.CALIBRATED_SCULK_SHRIEKER_ITEM.get());
                        }
                    }).build()) : null;
}
