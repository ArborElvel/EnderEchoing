package com.unddefined.enderechoing.blocks.entity;

import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EnderEchoicResonatorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float animationTime = 0;

    public EnderEchoicResonatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.ENDER_ECHOIC_RESONATOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnderEchoicResonatorBlockEntity blockEntity) {
        // 每秒让 sculkborne 判定一次：徘徊者有没有极小概率出现在仪器上
        if (!level.isClientSide && level.getGameTime() % 20 == 0) SculkBorneBridge.deviceTick(level, pos);

        // 更新动画时间
        if (level.isClientSide) blockEntity.animationTime += 1;

        //粒子效果
        if (level.isClientSide && level.getRandom().nextFloat() < 0.1) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5 + (level.random.nextDouble() - 0.1) * 0.2,
                    pos.getY() + 1 + (level.random.nextDouble() - 0.1) * 0.2,
                    pos.getZ() + 0.5 + (level.random.nextDouble() - 0.1) * 0.2,
                    (level.random.nextDouble() - 0.5),
                    -level.random.nextDouble(),
                    (level.random.nextDouble() - 0.5));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {return this.cache;}

    // 提供动画时间给渲染器使用
    public float getAnimationTime() {return animationTime;}
}
