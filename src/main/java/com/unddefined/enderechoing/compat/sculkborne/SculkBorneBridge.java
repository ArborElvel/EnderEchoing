package com.unddefined.enderechoing.compat.sculkborne;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class SculkBorneBridge {
    public static final String MOD_ID = "sculkborne";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation VEIL = id("sculk_veil");
    private static final ResourceLocation ECHO_DRUSE = id("echo_druse");
    private static final ResourceLocation RHYME_SHARD = id("rhyme_shard");
    private static final ResourceLocation CALIBRATED_SHRIEKER = id("calibrated_sculk_shrieker");

    /** sculkborne 里“传送后幽匿螨有几率出现”的钩子，反射调用，缺 mod 或方法时静默跳过。 */
    private static final String TELEPORT_HOOK_CLASS =
            "com.unddefined.sculkborne.compat.enderechoing.EnderEchoingTeleportHooks";
    private static final String TELEPORT_HOOK_METHOD = "onTeleport";

    private static Method teleportHook;
    private static boolean teleportHookUnavailable = false;

    /** sculkborne 里“徘徊者极小概率出现在末影回响仪器上”的钩子，反射调用，缺 mod 或方法时静默跳过。 */
    private static final String DEVICE_HOOK_CLASS =
            "com.unddefined.sculkborne.compat.enderechoing.EnderEchoingDeviceHooks";
    private static final String DEVICE_HOOK_METHOD = "onDeviceTick";

    private static Method deviceHook;
    private static boolean deviceHookUnavailable = false;

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

    /**
     * 传送成功后的通知：让 sculkborne 按自己的规则决定要不要在出发地与终点刷出幽匿螨。
     *
     * <p>幽匿螨不会自然生成，只在使用末影回响仪器传送后有几率出现，起点与终点各自判定一次，
     * 因此每个传送装置把玩家送到目的地之后都要调用一次本方法，
     * 并把传送前的位置一并带上（跨维度传送时它属于另一个维度）。
     * sculkborne 未安装或版本不匹配时什么也不做。
     *
     * @param player    刚被传送的玩家，调用时已经站在终点
     * @param fromLevel 出发地所在维度
     * @param fromPos   出发地坐标，即传送前玩家所在的位置
     */
    public static void afterTeleport(ServerPlayer player, Level fromLevel, Vec3 fromPos) {
        if (!isLoaded() || teleportHookUnavailable) return;

        Method method = teleportHook;

        if (method == null) {
            method = resolveTeleportHook();
            if (method == null) return;
        }

        try {
            method.invoke(null, player, fromLevel, fromPos);
        } catch (ReflectiveOperationException e) {
            teleportHookUnavailable = true;
            LOGGER.error("sculkborne failed to handle an EnderEchoing teleport", e);
        }
    }

    private static Method resolveTeleportHook() {
        try {
            teleportHook = Class.forName(TELEPORT_HOOK_CLASS)
                    .getMethod(TELEPORT_HOOK_METHOD, ServerPlayer.class, Level.class, Vec3.class);

            return teleportHook;
        } catch (ReflectiveOperationException | LinkageError e) {
            teleportHookUnavailable = true;

            return null;
        }
    }

    /**
     * 末影回响仪器的每秒判定：让 sculkborne 按自己的规则决定要不要在仪器上出现一只徘徊者。
     *
     * <p>只要仪器处于加载状态，每个仪器每秒调用一次本方法（见各仪器 BlockEntity 的 tick）；
     * 概率、短暂停留以及“没有目标就瞬移消失”都在 sculkborne 那边处理。
     * sculkborne 未安装或版本不匹配时什么也不做。
     *
     * @param level 仪器所在维度，客户端维度直接忽略
     * @param pos   仪器所在的方块位置
     */
    public static void deviceTick(Level level, BlockPos pos) {
        if (!isLoaded() || deviceHookUnavailable) return;
        if (level == null || level.isClientSide) return;

        Method method = deviceHook;

        if (method == null) {
            method = resolveDeviceHook();
            if (method == null) return;
        }

        try {
            method.invoke(null, level, pos);
        } catch (ReflectiveOperationException e) {
            deviceHookUnavailable = true;
            LOGGER.error("sculkborne failed to handle an EnderEchoing device tick", e);
        }
    }

    private static Method resolveDeviceHook() {
        try {
            deviceHook = Class.forName(DEVICE_HOOK_CLASS)
                    .getMethod(DEVICE_HOOK_METHOD, Level.class, BlockPos.class);

            return deviceHook;
        } catch (ReflectiveOperationException | LinkageError e) {
            deviceHookUnavailable = true;

            return null;
        }
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
