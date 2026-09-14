package com.unddefined.enderechoing.entities.ai;

import com.unddefined.enderechoing.entities.SculkSkeletonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * 幽匿骷髅调查振动的 Goal：接收到振动后寻路并走向振动源，直到振动源进入视野范围。
 *
 * <p>振动源由 {@link SculkSkeletonEntity#getVibrationSource()} 给出，接收半径与“距离过远”的过滤都在
 * {@link SculkSkeletonEntity} 的振动回调里完成，本 Goal 只负责走过去：
 *
 * <ul>
 *     <li>寻路失败（没有可行路径）时不会移动，直接放弃本次调查；</li>
 *     <li>振动源已经在视野范围内，或者已经锁定攻击目标（原版目标选择器在看得见生物时锁定目标）时也不移动。</li>
 * </ul>
 */
public class InvestigateVibrationGoal extends Goal {

    /** 移动速度倍率，与原版游荡 Goal 的 {@code WaterAvoidingRandomStrollGoal(this, 1.0F)} 一致。 */
    private static final double SPEED_MODIFIER = 1.0D;

    private final SculkSkeletonEntity mob;

    /** 本次调查的寻路目标，取自 {@link #canUse()} 时读到的振动源。 */
    @Nullable
    private BlockPos target;

    public InvestigateVibrationGoal(SculkSkeletonEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.mob.getVibrationSource() != null && this.mob.getTarget() == null;
    }

    @Override
    public void start() {
        BlockPos source = this.mob.getVibrationSource();
        if (source == null) return;

        // 振动源已经进入视野范围就不用特意过去，直接结束本次调查
        if (this.mob.isVibrationSourceVisible()) {
            this.mob.clearVibrationSource();
            return;
        }

        this.target = source;

        // 没有可行路径时不移动，放弃本次调查
        if (this.mob.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D,
                SPEED_MODIFIER)) return;

        this.mob.clearVibrationSource();
        this.target = null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || this.mob.getVibrationSource() == null) return false;

        // 已经锁定攻击目标（振动源多半就是它）时交给战斗用的 Goal
        if (this.mob.getTarget() != null) return false;

        // 振动源进入视野范围，或者路已经走完（到不了）就停下
        return !this.mob.isVibrationSourceVisible() && !this.mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.mob.clearVibrationSource();
        this.mob.getNavigation().stop();
        this.target = null;
    }
}
