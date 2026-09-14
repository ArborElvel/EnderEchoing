package com.unddefined.enderechoing;

import com.mojang.logging.LogUtils;
import com.unddefined.enderechoing.client.ModSoundEvents;
import com.unddefined.enderechoing.client.gui.TunerMenu;
import com.unddefined.enderechoing.entities.CreesperEntity;
import com.unddefined.enderechoing.entities.SculkSkeletonEntity;
import com.unddefined.enderechoing.entities.SculkSpreaderEntity;
import com.unddefined.enderechoing.entities.SculkZombieEntity;
import com.unddefined.enderechoing.server.events.SculkMobSpawnPlacements;
import com.unddefined.enderechoing.server.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

import static net.minecraft.world.level.Level.OVERWORLD;

// The @Mod annotation tells the loader that this class is the main mod class.
// The mod id is defined in mods.toml and must match the modId field below.
@Mod(EnderEchoing.MODID)
public class EnderEchoing {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "enderechoing";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final GlobalPos GZERO = new GlobalPos(OVERWORLD, BlockPos.ZERO);
    public static final UUID zeroUUID = new UUID(0, 0);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final Supplier<MenuType<TunerMenu>> TUNER_MENU = MENUS.register("tuner_menu", () -> IMenuTypeExtension.create(TunerMenu::new));
    public EnderEchoing(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        BlockRegistry.BLOCKS.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);
        EntityRegistry.ENTITIES.register(modEventBus);
        CreativeModeTabRegistry.CREATIVE_MODE_TABS.register(modEventBus);
        MobEffectRegistry.MOB_EFFECTS.register(modEventBus);
        PotionRegistry.POTIONS.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
        ParticlesRegistry.PARTICLE_TYPES.register(modEventBus);
        DataRegistry.COMPONENT_TYPES.register(modEventBus);
        DataRegistry.ATTACHMENT_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(EnderEchoing::registerEntityAttributes);
        modEventBus.addListener(SculkMobSpawnPlacements::registerSpawnPlacements);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.SCULK_SPREADER_ENTITY.get(), SculkSpreaderEntity.createAttributes().build());
        event.put(EntityRegistry.SCULK_ZOMBIE_ENTITY.get(), SculkZombieEntity.createAttributes().build());
        event.put(EntityRegistry.CREESPER_ENTITY.get(), CreesperEntity.createAttributes().build());
        event.put(EntityRegistry.SCULK_SKELETON_ENTITY.get(), SculkSkeletonEntity.createAttributes().build());
    }
}
