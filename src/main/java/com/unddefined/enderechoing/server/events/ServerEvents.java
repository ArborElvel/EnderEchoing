package com.unddefined.enderechoing.server.events;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.blocks.EnderEchoCrystalBlock;
import com.unddefined.enderechoing.compat.sculkborne.CompatSculkRegistry;
import com.unddefined.enderechoing.server.DataComponents.EnderEchoCrystalSavedData;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.EnderEchoingEyeLocator;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Comparator;

import static com.unddefined.enderechoing.Config.SCULK_VEIL_GLOWING_DURATION;
import static com.unddefined.enderechoing.EnderEchoing.LOGGER;
import static com.unddefined.enderechoing.compat.sculkborne.CompatSculkRegistry.*;
import static com.unddefined.enderechoing.server.registry.BlockRegistry.ENDER_ECHOIC_RESONATOR;
import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_AMOUNT;
import static com.unddefined.enderechoing.server.registry.DataRegistry.MARKED_POSITIONS_CACHE;
import static net.minecraft.world.effect.MobEffects.DARKNESS;
import static net.minecraft.world.effect.MobEffects.GLOWING;

@EventBusSubscriber(modid = EnderEchoing.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = MarkedPositionsManager.getManager(player).teleporters();
        data.removeIf(target -> {
            ServerLevel level = player.server.getLevel(target.dimension());
            if (level == null) return true;
            if (!level.getBlockState(target.pos()).is(ENDER_ECHOIC_RESONATOR.get())) {
                LOGGER.info("Removed invalid resonator at {}", target);
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CompatSculkRegistry.ACTIVE) tickCompatSculkVeil(player);
        if (player.tickCount % 20 == 0) EnderEchoingEyeLocator.markVisitedIfInside(player);
    }

    private static void tickCompatSculkVeil(ServerPlayer player) {
        long now = player.level().getGameTime();
        long start = player.getData(GLOWING_START);
        if (player.hasEffect(GLOWING)) {
            if (start < 0) player.setData(GLOWING_START, now);
            player.setData(GLOWING_LAST_TICK, now);
        } else if (start >= 0) {
            long lastTick = player.getData(GLOWING_LAST_TICK);
            player.setData(GLOWING_TOTAL, player.getData(GLOWING_TOTAL) + Math.max(0, lastTick - start + 1));
            player.setData(GLOWING_START, -1L);
            player.setData(GLOWING_LAST_TICK, -1L);
        }
        long glowingInProgress = player.getData(GLOWING_START) >= 0
                ? now - player.getData(GLOWING_START) + 1 : 0;
        if (player.getData(SCULK_VEIL_TOTAL) - player.getData(GLOWING_TOTAL) - glowingInProgress >= 6000
                && !player.hasEffect(DARKNESS)) {
            player.addEffect(new MobEffectInstance(DARKNESS, Integer.MAX_VALUE, 1, false, true));
        }
    }

    @SubscribeEvent
    public static void onExpireEffect(MobEffectEvent.Expired event) {
        if (!CompatSculkRegistry.ACTIVE) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && effect.is(CompatSculkRegistry.SCULK_VEIL)) {
            event.getEntity().addEffect(new MobEffectInstance(GLOWING, SCULK_VEIL_GLOWING_DURATION.get() * 20));
        }
    }

    @SubscribeEvent
    public static void onRemoveEffect(MobEffectEvent.Remove event) {
        if (!CompatSculkRegistry.ACTIVE) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && effect.is(CompatSculkRegistry.SCULK_VEIL)) {
            event.getEntity().setData(SCULK_VEIL_START, -1L);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        var target = event.getTarget();
        if (!(target instanceof EnderMan enderMan)) return;
        CuriosApi.getCuriosInventory(player).flatMap(handler -> handler.findCurios(ItemRegistry.ENDER_ECHOING_EYE.get())
                .stream().findFirst()).ifPresent(slot -> {
            double range = enderMan.getAttributeValue(Attributes.FOLLOW_RANGE) / 2;
            var aabb = AABB.unitCubeFromLowerCorner(enderMan.position()).inflate(range, 10.0F, range);
            player.level().getEntitiesOfClass(EnderMan.class, aabb, EntitySelector.NO_SPECTATORS).stream()
                    .filter(entity -> entity != enderMan)
                    .filter(entity -> entity.getTarget() == null)
                    .filter(entity -> !entity.isAlliedTo(player))
                    .forEach(entity -> entity.setTarget(player));
            slot.stack().shrink(1);
            player.level().playSound(player, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1f, 1f);
        });
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var pos = player.blockPosition();
        var level = player.level();
        if (!(level.getBlockState(pos).getBlock() instanceof EnderEchoCrystalBlock)) return;
        EnderEchoCrystalSavedData.get((ServerLevel) level).getAll().stream()
                .filter(crystal -> crystal.pos().dimension().equals(level.dimension())
                        && crystal.pos().pos().getX() == pos.getX()
                        && crystal.pos().pos().getZ() == pos.getZ()
                        && crystal.pos().pos().getY() > pos.getY())
                .min(Comparator.comparingInt(crystal -> crystal.pos().pos().getY()))
                .ifPresent(crystal -> player.teleportTo(crystal.pos().pos().getX() + 0.5,
                        crystal.pos().pos().getY() + 0.5, crystal.pos().pos().getZ() + 0.5));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var manager = player.getData(MARKED_POSITIONS_CACHE);
        var deaths = manager.markedPositions().stream().filter(position -> position.name().startsWith("☠")).toList();
        if (player.getData(EE_PEARL_AMOUNT) < 0 && deaths.size() <= 3) return;
        manager.markedPositions().removeIf(position -> position.name().startsWith("☠"));
        manager.addMarkedPosition(player.level().dimension(), player.blockPosition(),
                "☠" + Component.translatable("screen.enderechoing.last_death").getString() + "☠", 0, false);
        for (int i = 0; i < Math.min(3, deaths.size()); i++) {
            var old = deaths.get(i);
            String name = switch (i) {
                case 0 -> "☠" + Component.translatable("screen.enderechoing.previous_death").getString() + "☠";
                case 1 -> "☠" + Component.translatable("screen.enderechoing.earlier_death").getString() + "☠";
                default -> "☠" + Component.translatable("screen.enderechoing.even_earlier_death").getString() + "☠";
            };
            manager.addMarkedPosition(old.dimension(), old.pos(), name, 0, false);
        }
        if (deaths.size() < 4) player.setData(EE_PEARL_AMOUNT, player.getData(EE_PEARL_AMOUNT) - 1);
    }
}
