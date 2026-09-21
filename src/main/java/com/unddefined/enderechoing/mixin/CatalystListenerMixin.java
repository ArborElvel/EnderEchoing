package com.unddefined.enderechoing.mixin;

import com.unddefined.enderechoing.Config;
import com.unddefined.enderechoing.blocks.entity.EchoDruseBlockEntity;
import com.unddefined.enderechoing.compat.sculkborne.CompatSculkBloom;
import com.unddefined.enderechoing.compat.sculkborne.CompatSculkRegistry;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * sculkborne 未加载时的幽匿催发体兼容副本：让催发体上的回响晶簇吸收死亡经验生长，
 * 并让催发体上的幽匿尖啸体有几率获得 CAN_SUMMON。
 *
 * <p>两个模组同时加载时以 sculkborne 为权威，本副本直接放行给原版与 sculkborne 的实现。
 */
@Mixin(targets = "net/minecraft/world/level/block/entity/SculkCatalystBlockEntity$CatalystListener")
public class CatalystListenerMixin {

    @Final
    @Shadow
    private PositionSource positionSource;

    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void enderechoing$handleGameEvent(ServerLevel level, Holder<GameEvent> gameEvent, GameEvent.Context context,
                                               Vec3 pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkBorneBridge.isLoaded()) return;

        if (gameEvent.is(GameEvent.ENTITY_DIE) && context.sourceEntity() instanceof LivingEntity livingEntity
                && !livingEntity.wasExperienceConsumed()) {

            int experienceReward = livingEntity.getExperienceReward(level,
                    Optionull.map(livingEntity.getLastDamageSource(), DamageSource::getEntity));
            if (!livingEntity.shouldDropExperience() || experienceReward < 1) cir.setReturnValue(false);

            // 通过 positionSource 获取 Sculk Catalyst 的位置
            Optional<Vec3> catalystPosOpt = positionSource.getPosition(level);
            if (catalystPosOpt.isEmpty()) cir.setReturnValue(false);
            BlockPos catalystPos = BlockPos.containing(catalystPosOpt.get());
            // 检查上方是否有 EchoDruse 方块
            BlockState aboveState = level.getBlockState(catalystPos.above());

            if (aboveState.getBlock() == CompatSculkRegistry.ECHO_DRUSE_BLOCK.get()) {
                // 获取方块实体
                if (level.getBlockEntity(catalystPos.above()) instanceof EchoDruseBlockEntity echoDruse) {
                    if (echoDruse.getGrowthValue() <= Config.ECHO_DRUSE_MAX_GROWTH_VALUE.get()) {
                        // 增加 EchoDruse 的生长值
                        echoDruse.setGrowthValue(experienceReward);
                        // 标记经验已被消耗
                        livingEntity.skipDropExperience();
                        enderechoing$bloom(level, catalystPos);
                        cir.setReturnValue(true);
                    }
                }
            }

            if (level.getBlockEntity(catalystPos.above()) instanceof SculkShriekerBlockEntity shrieker
                    && !aboveState.getValue(SculkShriekerBlock.CAN_SUMMON)) {
                if (level.getRandom().nextInt(Config.SCULK_SHRIEKER_CAN_SUMMON_CHANCE.get()) == 0) {
                    level.setBlock(catalystPos.above(), aboveState.setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
                    var player = level.getNearestPlayer(catalystPos.getX(), catalystPos.getY(), catalystPos.getZ(), 8, false);
                    shrieker.tryShriek(level, (ServerPlayer) player);
                }
                livingEntity.skipDropExperience();
                enderechoing$bloom(level, catalystPos);
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private void enderechoing$bloom(ServerLevel level, BlockPos catalystPos) {
        BlockState catalystState = level.getBlockState(catalystPos);
        level.setBlock(catalystPos, catalystState.setValue(SculkCatalystBlock.PULSE, Boolean.TRUE), 3);
        level.scheduleTick(catalystPos, catalystState.getBlock(), 8);
        CompatSculkBloom.playBloomEffects(level, catalystPos);
    }
}
