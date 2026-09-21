package com.unddefined.enderechoing.compat.sculkborne;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * sculkborne 未加载时的幽匿催发体绽放兼容副本。
 *
 * <p>粒子与音效与原版催发体绽放（{@code CatalystListener#bloom}）一致，
 * 供 CatalystListenerMixin 在催发体位置播放。
 */
public final class CompatSculkBloom {
    /** 播放一次催发体绽放的粒子与音效 */
    public static void playBloomEffects(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                (double) pos.getX() + 0.5D,
                (double) pos.getY() + 1.15D,
                (double) pos.getZ() + 0.5D,
                2, 0.2D, 0.0D, 0.2D, 0.0D
        );
        level.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS,
                2.0F, 0.6F + level.getRandom().nextFloat() * 0.4F);
    }

    private CompatSculkBloom() {}
}
