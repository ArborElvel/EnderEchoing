package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.items.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("enderechoing");

    public static final DeferredItem<Item> ENDER_ECHOING_CORE = ITEMS.registerItem("ender_echoing_core", EnderEchoingCore::new);
    public static final DeferredItem<Item> ENDER_ECHO_TUNE_CHAMBER = ITEMS.registerItem("ender_echo_tune_chamber", EnderEchoTuneChamber::new);
    public static final DeferredItem<Item> ENDER_ECHO_CRYSTAL = ITEMS.registerItem("ender_echo_crystal", EnderEchoCrystal::new);
    public static final DeferredItem<Item> ENDER_ECHO_COMPASS = ITEMS.registerItem("ender_echo_compass", EnderEchoCompass::new);
    public static final DeferredItem<Item> ENDER_ECHOING_PEARL = ITEMS.registerItem("ender_echoing_pearl", EnderEchoingPearl::new);
    public static final DeferredItem<Item> ENDER_ECHOING_EYE = ITEMS.registerItem("ender_echoing_eye", EnderEchoingEye::new);
    public static final DeferredItem<Item> WARP_CORE = ITEMS.registerItem("warp_core", WarpCore::new);
}
