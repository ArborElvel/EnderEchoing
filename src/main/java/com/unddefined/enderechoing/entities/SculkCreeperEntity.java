package com.unddefined.enderechoing.entities;

import com.unddefined.enderechoing.server.InfrasoundDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.unddefined.enderechoing.Config.*;

public class SculkCreeperEntity extends Creeper implements GeoEntity, SculkMob {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int sculkHealCooldown;

    public SculkCreeperEntity(EntityType<SculkCreeperEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        applySunlightDebuffs();
        sculkHealCooldown = tickSculkRegeneration(sculkHealCooldown);
        tickSculkBlockBonus();
    }

    /**
     * 自爆时发出的次声波。
     *
     * <p>幽匿爬行者的自爆不做物理爆炸（由 {@code ServerEvents} 取消 ExplosionEvent.Start，
     * 原版爆炸的方块破坏、伤害、音效与粒子都不会发生），改为在自身位置结算一次次声波爆发：
     * 范围内生物受到真实伤害与次声波减益，范围与伤害取 {@link com.unddefined.enderechoing.Config} 中的配置值。
     *
     * <p>爆源自身不参与结算，与原版爆炸不伤害爆源生物一致。
     *
     * @param level 自爆所在的服务端维度
     */
    public void infrasoundExplode(ServerLevel level) {
        InfrasoundDamage.InfrasoundBurst(level, this.position(),
                SCULK_CREEPER_INFRASOUND_HURT_RANGE.getAsInt(),
                SCULK_CREEPER_INFRASOUND_AFFECT_RANGE.getAsInt(),
                SCULK_CREEPER_INFRASOUND_HURT_DAMAGE.getAsInt(),
                this, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 1,
                state -> state.isMoving()
                        ? state.setAndContinue(RawAnimation.begin().thenLoop("walk"))
                        : state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
