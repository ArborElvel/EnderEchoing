package com.unddefined.enderechoing.server.events;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.blocks.EnderEchoCrystalBlock;
import com.unddefined.enderechoing.entities.SculkCreeperEntity;
import com.unddefined.enderechoing.entities.SculkMob;
import com.unddefined.enderechoing.entities.SculkSkeletonEntity;
import com.unddefined.enderechoing.server.DataComponents.EnderEchoCrystalSavedData;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.EnderEchoingEyeLocator;
import com.unddefined.enderechoing.server.SculkBloom;
import com.unddefined.enderechoing.server.SculkIntrusionSpreader;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import com.unddefined.enderechoing.server.registry.PotionRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Comparator;

import static com.unddefined.enderechoing.Config.SCULK_VEIL_GLOWING_DURATION;
import static com.unddefined.enderechoing.EnderEchoing.LOGGER;
import static com.unddefined.enderechoing.effects.AttackScatteredEffect.attack_scattered_modifier_id;
import static com.unddefined.enderechoing.effects.StaggerEffect.stagger_modifier_id;
import static com.unddefined.enderechoing.effects.TinnitusEffect.tinnitus_modifier_id;
import static com.unddefined.enderechoing.server.registry.BlockRegistry.ENDER_ECHOIC_RESONATOR;
import static com.unddefined.enderechoing.server.registry.DataRegistry.*;
import static com.unddefined.enderechoing.server.registry.MobEffectRegistry.*;
import static com.unddefined.enderechoing.server.registry.TagRegistry.SCULK_BLOCKS;
import static net.minecraft.world.effect.MobEffects.DARKNESS;
import static net.minecraft.world.effect.MobEffects.GLOWING;
import static net.minecraft.world.entity.ai.attributes.Attributes.*;

@EventBusSubscriber(modid = EnderEchoing.MODID)
public class ServerEvents {
    private static final float SCULK_MATTER_BLOCK_DROP_CHANCE = 0.10F;
    private static final float FORTUNE_BLOCK_DROP_CHANCE = 0.10F;

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        // 幽匿脉络 + 粗制药水 → 幽匿侵扰药水
        event.getBuilder().addMix(Potions.AWKWARD, Items.SCULK_VEIN, PotionRegistry.SCULK_INTRUSION);
    }

    @SubscribeEvent
    public static void onEntityDeath(VanillaGameEvent event) {
        if (!event.getVanillaEvent().is(GameEvent.ENTITY_DIE)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getCause() instanceof LivingEntity dead)) return;
        if (!dead.shouldDropExperience() || dead.wasExperienceConsumed()) return;

        var damageSource = dead.getLastDamageSource();
        int xp = dead.getExperienceReward(level, damageSource == null ? null : damageSource.getEntity());
        if (xp <= 0) return;

        Vec3 deathPos = event.getEventPosition();
        // 先找作用盒覆盖到死亡点的主体（带侵扰效果或监守者），没有就不用抢这份经验
        double radius = SculkIntrusionSpreader.FOLLOW_RADIUS;
        var host = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(deathPos, radius * 2, radius * 2, radius * 2),
                h -> h.isAlive() && (h.hasEffect(SCULK_INTRUSION) || h instanceof Warden)
                        && SculkIntrusionSpreader.followBox(h).contains(deathPos)).stream().findFirst().orElse(null);
        if (host == null) return;
        // 附近有可用的幽匿催发体时让给它：它会在派发阶段吃掉这份死亡经验
        if (SculkIntrusionSpreader.hasUsableCatalystNearby(level, deathPos)) return;
        // 有几率由侵扰主体转化经验
        if (level.getRandom().nextFloat() >= ((host instanceof Warden) ? 0 : SculkIntrusionSpreader.TRIGGER_CHANCE)) return;
        host.getData(SCULK_SPREADER).absorbEntityDeath(level, host, deathPos, xp);
        dead.skipDropExperience();

    }

    /** 幽匿生物死亡时，按概率在死亡位置原地绽放一次幽匿催发体效果 */
    @SubscribeEvent
    public static void onSculkMobDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof SculkMob sculkMob) sculkMob.triggerSculkBloomOnDeath();
    }

    /** 幽匿生物的基础掉落（echo_shard、sculk_matter 等），抢夺附魔只提高掉落率 */
    @SubscribeEvent
    public static void onSculkMobDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof SculkMob sculkMob)) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        // 与原版一样遵守 doMobLoot 规则
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        sculkMob.dropSculkMobLoot(level, event.getSource(), event.getDrops());
    }

    /** 幽匿爬行者自爆时不做物理爆炸，改为发出次声波 */
    @SubscribeEvent
    public static void onSculkCreeperExplosion(ExplosionEvent.Start event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof SculkCreeperEntity creeper)) return;
        if (event.getLevel() instanceof ServerLevel level) creeper.infrasoundExplode(level);
        event.setCanceled(true);
    }

    /** 破坏幽匿系方块时按概率掉落一个幽匿物质，概率受时运影响。 */
    @SubscribeEvent
    public static void onSculkBlockDrops(BlockDropsEvent event) {
        if (!event.getState().is(SCULK_BLOCKS)) return;

        var fortune = event.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE);
        int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(fortune, event.getTool());
        float chance = Math.min(1.0F, SCULK_MATTER_BLOCK_DROP_CHANCE + fortuneLevel * FORTUNE_BLOCK_DROP_CHANCE);
        if (event.getLevel().getRandom().nextFloat() >= chance) return;

        ItemEntity drop = new ItemEntity(event.getLevel(), event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                new ItemStack(ItemRegistry.SCULK_MATTER.get()));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    /** 驱动尚未结束的幽匿绽放 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) SculkBloom.serverTick(level);
    }

    /** 维度卸载时丢弃其上未完成的幽匿绽放 */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) SculkBloom.discard(level);
    }

    /**
     * 监守者击中目标后为目标赋予侵扰效果（覆盖近战与音爆伤害）
     */
    @SubscribeEvent
    public static void onWardenAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Warden)) return;
        if (!(event.getEntity().level() instanceof ServerLevel)) return;
        event.getEntity().addEffect(new MobEffectInstance(SCULK_INTRUSION, 20 * 60));
    }

    /**
     * 幽匿生物击中目标后有几率为目标赋予侵扰效果（近战与算作该生物造成的伤害都包含在内）
     */
    @SubscribeEvent
    public static void onSculkMobAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof SculkMob sculkMob)) return;
        if (!(event.getEntity().level() instanceof ServerLevel)) return;
        sculkMob.tryApplyIntrusionOnAttack(event.getEntity());
    }

    /**
     * 幽匿骷髅击中目标后使其失明、失聪（近战与它射出的箭都包含在内）
     */
    @SubscribeEvent
    public static void onSculkSkeletonAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof SculkSkeletonEntity skeleton)) return;
        if (!(event.getEntity().level() instanceof ServerLevel)) return;
        skeleton.applyBlindnessAndDeafnessOnHit(event.getEntity());
    }

    /**
     * 监守者每 tick 的驱动：常驻的侵扰 spreader 在没有侵扰效果时也每 tick 驱动
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Warden warden)) return;
        if (!(warden.level() instanceof ServerLevel level)) return;
        if (!warden.isAlive()) return;

        if (warden.hasEffect(SCULK_INTRUSION)) return;
        warden.getData(SCULK_SPREADER).serverTick(level, warden);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = MarkedPositionsManager.getManager(player).teleporters();
        // 使用 removeIf 安全地过滤并删除无效数据
        data.removeIf(T -> {
            ServerLevel target = player.server.getLevel(T.dimension());

            // 如果 target 为 null，或者方块不是预期的，则返回 true 进行删除
            if (target == null) return true;
            if (!target.getBlockState(T.pos()).is(ENDER_ECHOIC_RESONATOR.get())) {
                LOGGER.info("Removed invalid resonator at {}", T);
                return true; // 返回 true 表示移除该元素
            }
            return false; // 返回 false 表示保留该元素
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        updateGlowingTime(player, now);
        long glowingInProgress = player.getData(GLOWING_START) >= 0 ? now - player.getData(GLOWING_START) + 1 : 0;
        if (player.getData(SCULK_VEIL_TOTAL) - player.getData(GLOWING_TOTAL) - glowingInProgress >= 6000
                && !player.hasEffect(DARKNESS))
            player.addEffect(new MobEffectInstance(DARKNESS, Integer.MAX_VALUE, 1, false, true));
        if (player.tickCount % 20 != 0) return;
        EnderEchoingEyeLocator.markVisitedIfInside(player);
    }

    private static void updateGlowingTime(ServerPlayer player, long now) {
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
    }

    @SubscribeEvent
    public static void onExpireEffect(MobEffectEvent.Expired event) {
        var E = event.getEntity();
        if (!E.hasEffect(SCULK_INTRUSION)) E.getData(SCULK_SPREADER).clear();

        if (!E.hasEffect(TINNITUS) && E.getAttribute(FOLLOW_RANGE) != null) {
            if (E instanceof Monster monster) monster.getAttribute(FOLLOW_RANGE).removeModifier(tinnitus_modifier_id);
        }
        if (!E.hasEffect(STAGGER) && E.getAttribute(MOVEMENT_SPEED) != null) {
            E.getAttribute(MOVEMENT_SPEED).removeModifier(stagger_modifier_id);
        }
        if (!E.hasEffect(ATTACK_SCATTERED) && E.getAttribute(ATTACK_SPEED) != null) {
            E.getAttribute(ATTACK_SPEED).removeModifier(attack_scattered_modifier_id);
        }
        if (!E.hasEffect(SCULK_VEIL)) E.addEffect(new MobEffectInstance(GLOWING, SCULK_VEIL_GLOWING_DURATION.get() * 20));

    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.hasEffect(STAGGER)) {
            // 获取当前移动输入
            var movement = event.getInput();

            // 获取效果等级（用于确定偏移程度）
            int amplifier = player.getEffect(STAGGER).getAmplifier();

            // 随机偏移移动方向
            RandomSource random = player.getRandom();
            float offsetStrength = 0.1f * (amplifier + 1); // 等级越高偏移越严重

            // 添加随机偏移
            movement.forwardImpulse += (random.nextFloat() - 0.5f) * offsetStrength;
            movement.leftImpulse += (random.nextFloat() - 0.5f) * offsetStrength;
        }

    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ATTACK_SCATTERED)) {
            // 获取效果实例
            MobEffectInstance effectInstance = entity.getEffect(ATTACK_SCATTERED);
            if (effectInstance != null) {
                int amplifier = effectInstance.getAmplifier();

                // 根据效果等级有概率取消攻击
                RandomSource random = entity.getRandom();
                float chance = 0.3f * (amplifier + 1); // 每级增加5%的概率
                if (random.nextFloat() < chance) {
                    // 取消攻击
                    event.setCanceled(true);
                }
            }
        }

    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        var target = event.getTarget();
        if (!(target instanceof EnderMan enderMan)) return;
        CuriosApi.getCuriosInventory(player).flatMap(h -> h.findCurios(ItemRegistry.ENDER_ECHOING_EYE.get())
                .stream().findFirst()).ifPresent(slot -> {
            double d0 = enderMan.getAttributeValue(Attributes.FOLLOW_RANGE) / 2;
            var aabb = AABB.unitCubeFromLowerCorner(enderMan.position()).inflate(d0, 10.0F, d0);
            player.level().getEntitiesOfClass(EnderMan.class, aabb, EntitySelector.NO_SPECTATORS).stream()
                    .filter(e -> e != enderMan).filter(e -> e.getTarget() == null)
                    .filter(e -> !e.isAlliedTo(player)).forEach(e -> e.setTarget(player));
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
        EnderEchoCrystalSavedData.get((ServerLevel) level).getAll()
                .stream().filter(p -> p.pos().dimension().equals(level.dimension()) && p.pos().pos().getX() == pos.getX() && p.pos().pos().getZ() == pos.getZ() && p.pos().pos().getY() > pos.getY())
                .min(Comparator.comparingInt(p -> p.pos().pos().getY()))
                .ifPresent(p -> player.teleportTo(p.pos().pos().getX() + 0.5, p.pos().pos().getY() + 0.5, p.pos().pos().getZ() + 0.5));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var manager = player.getData(MARKED_POSITIONS_CACHE);
        // 取出所有死亡点（按原顺序）
        var deaths = manager.markedPositions().stream().filter(p -> p.name().startsWith("☠")).toList();
        if (player.getData(EE_PEARL_AMOUNT) < 0 && deaths.size() <= 3) return;
        // 先删掉所有旧的死亡点（准备重建）
        manager.markedPositions().removeIf(p -> p.name().startsWith("☠"));
        // 最新的死亡点，直接加
        manager.addMarkedPosition(player.level().dimension(), player.blockPosition(),
                "☠" + Component.translatable("screen.enderechoing.last_death").getString() + "☠", 0, false);
        // 之前的死亡点依次下沉，最多保留 3 个
        for (int i = 0; i < Math.min(3, deaths.size()); i++) {
            var old = deaths.get(i);
            String name = "";
            switch (i) {
                case 0 -> name = "☠" + Component.translatable("screen.enderechoing.previous_death").getString() + "☠";
                case 1 -> name = "☠" + Component.translatable("screen.enderechoing.earlier_death").getString() + "☠";
                case 2 -> name = "☠" + Component.translatable("screen.enderechoing.even_earlier_death").getString() + "☠";
            }
            manager.addMarkedPosition(old.dimension(), old.pos(), name, 0, false);
        }
        // 消耗珍珠
        if (deaths.size() < 4) player.setData(EE_PEARL_AMOUNT, player.getData(EE_PEARL_AMOUNT) - 1);
    }

}
