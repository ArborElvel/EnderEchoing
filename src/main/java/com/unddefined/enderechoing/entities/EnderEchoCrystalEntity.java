package com.unddefined.enderechoing.entities;

import com.unddefined.enderechoing.server.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EnderEchoCrystalEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<BlockPos> OWNER_POS = SynchedEntityData.defineId(EnderEchoCrystalEntity.class, EntityDataSerializers.BLOCK_POS);

    public EnderEchoCrystalEntity(Level level, BlockPos ownerPos) {
        this(EntityRegistry.ENDER_ECHO_CRYSTAL_ENTITY.get(), level);
        this.entityData.set(OWNER_POS, ownerPos);
        this.setPos(ownerPos.getX() + 0.5, ownerPos.getY() + 0.5, ownerPos.getZ() + 0.5);
    }

    public EnderEchoCrystalEntity(EntityType<EnderEchoCrystalEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isPickable() {return true;}

    @Override
    public boolean isAttackable() {return true;}

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (source.getEntity() instanceof EnderDragon) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level().isClientSide) {
                this.remove(RemovalReason.KILLED);
                this.level().removeBlock(this.entityData.get(OWNER_POS), false);
                if (!source.is(DamageTypeTags.IS_EXPLOSION)) {
                    DamageSource d = source.getEntity() != null ? this.damageSources().explosion(this, source.getEntity()) : null;
                    this.level().explode(this, d, null, this.getX(), this.getY(), this.getZ(), 6.0F, false, Level.ExplosionInteraction.BLOCK);
                }
//                this.onDestroyedBy(source);
            }
            return true;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_POS, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerPos")) this.entityData.set(OWNER_POS, BlockPos.of(tag.getLong("OwnerPos")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("OwnerPos", this.entityData.get(OWNER_POS).asLong());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state ->
                        state.setAndContinue(RawAnimation.begin().thenLoop("ender_echo_crystal.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}

