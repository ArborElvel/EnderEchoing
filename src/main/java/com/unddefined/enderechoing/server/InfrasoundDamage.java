package com.unddefined.enderechoing.server;

import com.unddefined.enderechoing.network.packet.InfrasoundParticlePacket;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static com.unddefined.enderechoing.server.registry.ItemRegistry.RHYME_SHARD;
import static com.unddefined.enderechoing.server.registry.MobEffectRegistry.*;
import static net.minecraft.world.effect.MobEffects.CONFUSION;
import static net.minecraft.world.item.Items.ECHO_SHARD;

public class InfrasoundDamage extends DamageSource {
    public static final ResourceKey<DamageType> INFRASOUND_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("enderechoing", "infrasound_damage"));

    public InfrasoundDamage(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 damageSourcePosition) {
        super(type, directEntity, causingEntity, damageSourcePosition);
    }

    public static void InfrasoundBurst(ServerLevel level, Vec3 center, float hurt_range, float affect_range, int damage, Entity causingEntity) {
        // 获取范围内的所有生物实体
        var entities = level.getEntitiesOfClass(LivingEntity.class,
                net.minecraft.world.phys.AABB.ofSize(center, affect_range * 2, affect_range * 2, affect_range * 2));

        for (LivingEntity entity : entities) {
            // 计算实体与中心点的距离
            double distanceSqrt = entity.position().distanceTo(center);
            boolean inHurtRange = distanceSqrt <= hurt_range;
            boolean inAffectRange = distanceSqrt <= affect_range;

            // 手持龙韵碎片的生物消耗1个碎片，免疫此次次声波爆发对其的全部影响
            if ((inHurtRange || inAffectRange) && consumeRhymeShard(entity)) continue;

            // 对在半径hurt_range范围内的生物造成真实伤害
            if (inHurtRange) {
                // 创建伤害源
                var damageTypeHolder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(INFRASOUND_DAMAGE);
                var damageSource = new InfrasoundDamage(damageTypeHolder, null, causingEntity, center);

                // 造成damage点真实伤害（忽略护甲）
                entity.hurt(damageSource, damage);
                if (entity instanceof Player player)
                    player.getWardenSpawnTracker().ifPresent(t -> t.setWarningLevel(t.getWarningLevel() - 1));
                if (entity instanceof Warden W) W.hurt(damageSource, W.getHealth() * damage / 120f);
            }

            // 对在affect_range范围内的生物应用debuff效果
            if (inAffectRange) {
                // 计算持续时间 = affect_range - 与中心的距离
                int duration = (int) (affect_range - distanceSqrt);

                // 手持回响碎片的生物消耗1个碎片免疫此次debuff，但伤害仍照常结算
                if (!consumeEchoShard(entity)) {
                    // 应用多种debuff效果
                    entity.addEffect(new MobEffectInstance(ATTACK_SCATTERED, duration * 20, 1));
                    entity.addEffect(new MobEffectInstance(STAGGER, duration * 20, 1));
                    entity.addEffect(new MobEffectInstance(TINNITUS, duration * 20, 1));
                    entity.addEffect(new MobEffectInstance(CONFUSION, duration * 20, 1));
                   if(level.getRandom().nextInt(3) == 0) entity.addEffect(new MobEffectInstance(SCULK_INTRUSION, duration * 20, 1));
                }
            }

        }

        // 向所有客户端发送粒子效果数据包
        PacketDistributor.sendToAllPlayers(new InfrasoundParticlePacket(center, affect_range, false));
    }

    // 优先消耗主手的龙韵碎片，主手没有时消耗副手的；成功消耗返回true
    private static boolean consumeRhymeShard(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        if (mainHand.is(RHYME_SHARD)) {
            mainHand.shrink(1);
            return true;
        }
        ItemStack offHand = entity.getOffhandItem();
        if (offHand.is(RHYME_SHARD)) {
            offHand.shrink(1);
            return true;
        }
        return false;
    }

    // 优先消耗主手的回响碎片，主手没有时消耗副手的；成功消耗返回true
    private static boolean consumeEchoShard(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        if (mainHand.is(ECHO_SHARD)) {
            mainHand.shrink(1);
            return true;
        }
        ItemStack offHand = entity.getOffhandItem();
        if (offHand.is(ECHO_SHARD)) {
            offHand.shrink(1);
            return true;
        }
        return false;
    }
}
