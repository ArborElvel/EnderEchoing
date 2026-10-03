package com.unddefined.enderechoing.compat.sculkborne;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * sculkborne 兼容桥。
 *
 * <p>只负责「装没装 sculkborne」以及按注册表 id 取它的内容。传送与仪器判定已经改成对外事件
 * （{@link com.unddefined.enderechoing.api.event.EnderEchoTeleportEvent.Post} 与
 * {@link com.unddefined.enderechoing.api.event.EnderEchoDeviceEvent.Tick}），由 sculkborne 侧监听，
 * 不再需要反射调用它的钩子。
 */
public final class SculkBorneBridge {
    public static final String MOD_ID = "sculkborne";

    private static final ResourceLocation VEIL = id("sculk_veil");
    private static final ResourceLocation ECHO_DRUSE = id("echo_druse");
    private static final ResourceLocation RHYME_SHARD = id("rhyme_shard");
    private static final ResourceLocation CALIBRATED_SHRIEKER = id("calibrated_sculk_shrieker");

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static Holder<MobEffect> veilEffect() {
        if (isLoaded()) {
            return BuiltInRegistries.MOB_EFFECT.getHolder(VEIL)
                    .orElseThrow(() -> new IllegalStateException("Missing required effect " + VEIL));
        }
        return CompatSculkRegistry.SCULK_VEIL;
    }

    public static boolean hasVeil(LivingEntity entity) {
        return entity.hasEffect(veilEffect());
    }

    public static Item echoDruseItem() {
        return externalItem(ECHO_DRUSE, CompatSculkRegistry.ECHO_DRUSE);
    }

    public static Item rhymeShardItem() {
        return externalItem(RHYME_SHARD, CompatSculkRegistry.RHYME_SHARD);
    }

    public static Item calibratedShriekerItem() {
        return externalItem(CALIBRATED_SHRIEKER, CompatSculkRegistry.CALIBRATED_SCULK_SHRIEKER_ITEM);
    }

    public static Block calibratedShriekerBlock() {
        if (isLoaded()) {
            return BuiltInRegistries.BLOCK.getHolder(CALIBRATED_SHRIEKER)
                    .orElseThrow(() -> new IllegalStateException("Missing required block " + CALIBRATED_SHRIEKER)).value();
        }
        return CompatSculkRegistry.CALIBRATED_SCULK_SHRIEKER.get();
    }

    public static boolean isCalibratedShrieker(BlockState state) {
        return state.is(calibratedShriekerBlock());
    }

    private static Item externalItem(ResourceLocation id, Holder<? extends Item> fallback) {
        if (isLoaded()) {
            return BuiltInRegistries.ITEM.getHolder(id)
                    .orElseThrow(() -> new IllegalStateException("Missing required item " + id)).value();
        }
        return fallback.value();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private SculkBorneBridge() {}
}
