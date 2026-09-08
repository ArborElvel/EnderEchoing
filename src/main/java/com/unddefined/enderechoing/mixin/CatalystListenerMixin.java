package com.unddefined.enderechoing.mixin;

import com.unddefined.enderechoing.Config;
import com.unddefined.enderechoing.blocks.entity.EchoDruseBlockEntity;
import com.unddefined.enderechoing.server.registry.BlockRegistry;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

@Mixin(targets = "net/minecraft/world/level/block/entity/SculkCatalystBlockEntity$CatalystListener")
public class CatalystListenerMixin {

    @Final
    @Shadow
    private PositionSource positionSource;

    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void handleGameEvent(ServerLevel level, Holder<GameEvent> gameEvent, GameEvent.Context context, Vec3 pos, CallbackInfoReturnable<Boolean> cir) {
        if (gameEvent.is(GameEvent.ENTITY_DIE) && context.sourceEntity() instanceof LivingEntity livingEntity
                && !livingEntity.wasExperienceConsumed()) {

            int experienceReward = livingEntity.getExperienceReward(level, Optionull.map(livingEntity.getLastDamageSource(), DamageSource::getEntity));
            if (!livingEntity.shouldDropExperience() || experienceReward < 1) cir.setReturnValue(false);

            // 通过positionSource获取Sculk Catalyst的位置
            Optional<Vec3> catalystPosOpt = positionSource.getPosition(level);
            if (catalystPosOpt.isEmpty()) cir.setReturnValue(false);
            BlockPos catalystPos = BlockPos.containing(catalystPosOpt.get());
            // 检查上方是否有EchoDruse方块
            BlockState aboveState = level.getBlockState(catalystPos.above());

            if (aboveState.getBlock() == BlockRegistry.ECHO_DRUSE.get()) {
                // 获取方块实体
                if (level.getBlockEntity(catalystPos.above()) instanceof EchoDruseBlockEntity echoDruseEntity) {
                    if (echoDruseEntity.getGrowthValue() <= Config.ECHO_DRUSE_MAX_GROWTH_VALUE.get()) {
                        // 增加EchoDruse的生长值
                        echoDruseEntity.setGrowthValue(experienceReward);
                        // 标记经验已被消耗
                        livingEntity.skipDropExperience();
                        enderEchoing$bloom(level, catalystPos);
                        cir.setReturnValue(true);
                    }
                }
            }

            if (level.getBlockEntity(catalystPos.above()) instanceof SculkShriekerBlockEntity B && !aboveState.getValue(SculkShriekerBlock.CAN_SUMMON)) {
                if (level.getRandom().nextInt(Config.SCULK_SHRIEKER_CAN_SUMMON_CHANCE.get()) == 0) {
                    level.setBlock(catalystPos.above(), aboveState.setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
                    var player = level.getNearestPlayer(catalystPos.getX(), catalystPos.getY(), catalystPos.getZ(), 8, false);
                    B.tryShriek(level, (ServerPlayer)player);
                }
                livingEntity.skipDropExperience();
                enderEchoing$bloom(level, catalystPos);
                cir.setReturnValue(true);
            }

        }
    }

    @Unique
    private void enderEchoing$bloom(ServerLevel level, BlockPos catalystPos) {
        BlockState catalystState = level.getBlockState(catalystPos);
        level.setBlock(catalystPos, catalystState.setValue(SculkCatalystBlock.PULSE, Boolean.TRUE), 3);
        level.scheduleTick(catalystPos, catalystState.getBlock(), 8);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                (double) catalystPos.getX() + 0.5,
                (double) catalystPos.getY() + 1.15,
                (double) catalystPos.getZ() + 0.5,
                2, 0.2, 0.0, 0.2, 0.0
        );
        level.playSound(null, catalystPos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.0F, 0.6F + level.getRandom().nextFloat() * 0.4F);

    }
}
