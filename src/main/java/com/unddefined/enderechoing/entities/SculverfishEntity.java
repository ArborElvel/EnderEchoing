package com.unddefined.enderechoing.entities;

import com.unddefined.enderechoing.entities.ai.SculverfishBurrowGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 幽匿蠹虫（Sculverfish）。
 *
 * <p>基类是原版蠹虫，保留它的小体型与移动方式；作为幽匿生物实现 {@link SculkMob}，
 * 因此自动接入幽匿生物的共用约定（幽匿系方块上的回血与属性加成、阳光下的虚弱与缓慢、
 * 死亡时的幽匿绽放、基础掉落、次声波压制、攻击附带幽匿侵扰等）。
 *
 * <p>自己的行为：只生成在完整的幽匿系方块上，常态潜伏在幽匿块中并在相邻幽匿块之间移动；
 * 玩家进入 {@value #VIEW_RANGE} 格视野后钻出攻击，目标丢失或地面停留超时后再钻回幽匿块，
 * 在幽匿块上受伤时也会优先钻地；这些反应共同形成钻出攻击、钻地换位的游击循环。
 * 振动接收尚未实现。
 */
public class SculverfishEntity extends Silverfish implements GeoEntity, SculkMob {
    /** 视觉/索敌范围（格），同时用于钻出判定和原版 {@link Attributes#FOLLOW_RANGE}。 */
    public static final double VIEW_RANGE = 4.0D;

    /** 潜伏时允许在锚点周围移动/追击的半径（格）。 */
    public static final int HOME_RADIUS = 8;

    /** 地面上最长停留时间（tick），超过后强制钻回幽匿块，形成游击循环。 */
    private static final int MAX_SURFACE_TICKS = 60;

    /** 目标丢失后经过多少 tick 钻回地下。 */
    private static final int BURROW_AFTER_TARGET_LOST_TICKS = 20;

    private static final EntityDataAccessor<Integer> DATA_BURROW_STATE =
            SynchedEntityData.defineId(SculverfishEntity.class, EntityDataSerializers.INT);

    /** 潜伏/钻出/钻回的服务器状态，客户端通过 {@link #DATA_BURROW_STATE} 读取。 */
    public enum BurrowState {
        BURROWED,
        EMERGING,
        ACTIVE,
        BURROWING;

        private static final BurrowState[] STATES = values();

        private static BurrowState byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < STATES.length ? STATES[ordinal] : ACTIVE;
        }

        private static BurrowState byName(String name) {
            for (BurrowState state : STATES) {
                if (state.name().equals(name)) return state;
            }
            return ACTIVE;
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 当前潜伏的幽匿块；没有有效锚点时不会进入潜伏状态。 */
    @Nullable
    private BlockPos burrowAnchor;

    /** 幽匿系方块上的回血剩余计时（tick），见 {@link SculkMob#tickSculkRegeneration(int)}。 */
    private int sculkHealCooldown;

    /** 地面上已经停留的时间，用于强制钻回。 */
    private int surfaceTicks;

    /** 连续没有目标的 tick 数，用于目标丢失后钻回。 */
    private int ticksWithoutTarget;

    /**
     * 受伤后的钻地请求，由 {@link SculverfishBurrowGoal} 消费。
     *
     * <p>在幽匿块上受伤时置位：地面活动时立刻钻回幽匿块，已经潜伏时立刻在地下换位。
     */
    private boolean burrowRequested;

    /** 新生成实体首次加入世界后只初始化一次；读档实体在 NBT 读取时直接标记为已初始化。 */
    private boolean spawnInitialized;

    public SculverfishEntity(EntityType<SculverfishEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes().add(Attributes.FOLLOW_RANGE, VIEW_RANGE);
    }

    @Override
    protected void registerGoals() {
        // 不调用 super.registerGoals()：原版蠹虫会钻入被虫蚀方块并唤醒同类，
        // 这两条都与“只居住在幽匿块中”冲突，这里显式建立自己的 Goal 列表。
        goalSelector.addGoal(0, new SculverfishBurrowGoal(this));
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BURROW_STATE, BurrowState.ACTIVE.ordinal());
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide && getBurrowState() == BurrowState.ACTIVE) {
            surfaceTicks++;
            if (getTarget() == null) {
                ticksWithoutTarget++;
            } else {
                ticksWithoutTarget = 0;
            }
        }

        // 阳光直射下获得虚弱与缓慢
        applySunlightDebuffs();
        // 站在幽匿系方块上时按亮度反比缓慢回血
        sculkHealCooldown = tickSculkRegeneration(sculkHealCooldown);
        // 站在幽匿系方块上时临时提高移动速度与生命上限
        tickSculkBlockBonus();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        // 受伤时优先钻地：只要身上/脚下还是幽匿块，就放下当前行为先钻回地下
        if (damaged && !level().isClientSide && isOnSculkBlock()) {
            burrowRequested = true;
        }
        return damaged;
    }

    @Override
    public boolean isPickable() {
        return getBurrowState() == BurrowState.ACTIVE && super.isPickable();
    }

    @Override
    public boolean isAttackable() {
        return getBurrowState() == BurrowState.ACTIVE && super.isAttackable();
    }

    @Override
    public boolean isPushable() {
        return getBurrowState() == BurrowState.ACTIVE && super.isPushable();
    }

    /**
     * 新生成实体首次加入世界后初始化潜伏状态。
     *
     * <p>由 {@code ServerEvents} 监听 {@code EntityJoinLevelEvent} 调用；读档实体在
     * {@link #readAdditionalSaveData(CompoundTag)} 中已经标记为初始化，不会重新潜伏。
     */
    public void initializeSpawnIfNeeded() {
        if (spawnInitialized) return;
        spawnInitialized = true;

        // 原版会给 FOLLOW_RANGE 加随机的生成加成，这里移除以保证视觉严格不超过 4 格
        AttributeInstance followRange = getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) followRange.removeModifier(RANDOM_SPAWN_BONUS_ID);

        BlockPos anchor = findSculkAnchor(level(), blockPosition(), 2, 2);
        if (anchor != null) {
            setBurrowAnchor(anchor);
            setBurrowState(BurrowState.BURROWED);
            moveToBurrowCenter(anchor);
        } else setBurrowState(BurrowState.ACTIVE);

    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("BurrowState", getBurrowState().name());
        if (burrowAnchor != null) compound.putLong("BurrowAnchor", burrowAnchor.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        spawnInitialized = true;

        if (compound.contains("BurrowAnchor")) {
            setBurrowAnchor(BlockPos.of(compound.getLong("BurrowAnchor")));
        }

        BurrowState state = BurrowState.byName(compound.getString("BurrowState"));
        if (state != BurrowState.ACTIVE) {
            BlockPos anchor = burrowAnchor;
            if (anchor == null || !isFullSculkBlock(level(), anchor)) {
                anchor = findSculkAnchor(level(), blockPosition(), 8, 4);
            }
            if (anchor != null) {
                setBurrowAnchor(anchor);
                setBurrowState(BurrowState.BURROWED);
                moveToBurrowCenter(anchor);
                return;
            }
        }

        if (burrowAnchor != null && !isFullSculkBlock(level(), burrowAnchor)) {
            burrowAnchor = null;
        }
        setBurrowState(BurrowState.ACTIVE);
        if (burrowAnchor != null) restrictTo(burrowAnchor, HOME_RADIUS);
    }

    /** 当前潜伏/钻出/钻回状态。 */
    public BurrowState getBurrowState() {
        return BurrowState.byOrdinal(entityData.get(DATA_BURROW_STATE));
    }

    /** 是否完全潜伏在幽匿块内。 */
    public boolean isBurrowed() {
        return getBurrowState() == BurrowState.BURROWED;
    }

    /** 当前潜伏锚点，没有有效锚点时返回 {@code null}。 */
    @Nullable
    public BlockPos getBurrowAnchor() {
        return burrowAnchor;
    }

    /** 设置潜伏锚点，并把它作为不会离开幽匿区域的限制中心。 */
    public void setBurrowAnchor(BlockPos anchor) {
        this.burrowAnchor = anchor.immutable();
        restrictTo(this.burrowAnchor, HOME_RADIUS);
    }

    /** 切换钻地状态；状态同步给客户端，并统一处理无碰撞、无重力和可选中状态。 */
    public void setBurrowState(BurrowState state) {
        entityData.set(DATA_BURROW_STATE, state.ordinal());
        applyBurrowState(state);
    }

    /** 读档或外部恢复状态后重新应用一次物理/AI 标记。 */
    public void refreshBurrowState() {
        applyBurrowState(getBurrowState());
    }

    private void applyBurrowState(BurrowState state) {
        boolean active = state == BurrowState.ACTIVE;
        this.noPhysics = !active;
        this.setNoGravity(!active);
        this.surfaceTicks = 0;
        this.ticksWithoutTarget = 0;
        if (!active) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setTarget(null);
            this.getNavigation().stop();
        } else {
            // 回到地面说明钻地请求已经处理完（成功钻出或找不到落点），不再保留
            this.burrowRequested = false;
        }
    }

    /** 地面停留超时、目标丢失或受伤是否已经满足钻回条件，由 {@link SculverfishBurrowGoal} 查询。 */
    public boolean shouldBurrow() {
        return burrowRequested
                || surfaceTicks >= MAX_SURFACE_TICKS
                || ticksWithoutTarget >= BURROW_AFTER_TARGET_LOST_TICKS;
    }

    /** 取出并清除受伤钻地请求；只在 {@link SculverfishBurrowGoal} 处理该请求时调用。 */
    public boolean consumeBurrowRequest() {
        boolean requested = burrowRequested;
        this.burrowRequested = false;
        return requested;
    }

    /** 潜伏位置 {@value #VIEW_RANGE} 格内的候选玩家：存活、非创造、非旁观者。 */
    @Nullable
    public Player findNearbyPlayer() {
        if (!(level() instanceof ServerLevel serverLevel)) return null;

        return serverLevel.getNearestPlayer(getX(), getEyeY(), getZ(), VIEW_RANGE,
                entity -> entity instanceof Player candidate
                        && candidate.isAlive()
                        && !candidate.isCreative()
                        && !candidate.isSpectator());
    }

    /** 从指定钻出落点能否看见该玩家；落点本身是无碰撞的，射线起点不会落在方块内部。 */
    public boolean canSeeFrom(Vec3 emergePos, Player player) {
        HitResult hit = level().clip(new ClipContext(emergePos, player.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    /**
     * 从指定幽匿块钻出的落点：优先从上方钻出，上方被挡住时看四个侧面。
     *
     * <p>落点要容得下它，并且下方必须是实心方块（钻出来得有地方站）；不要求落点上方是空的，
     * 所以贴着别的方块也能从侧面钻出来。没有任何落点时返回 {@code null}。
     */
    @Nullable
    public Vec3 findEmergePos(BlockPos anchor) {
        if (!isFullSculkBlock(level(), anchor)) return null;

        Vec3 top = emergencePos(anchor);
        if (canFitAt(top)) return top;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = anchor.relative(direction);
            if (!isSolidBlock(level(), side.below())) continue;

            Vec3 sidePos = sideEmergePos(anchor, direction);
            if (canFitAt(sidePos)) return sidePos;
        }

        return null;
    }

    /** 落点是否容得下它（不与方块碰撞）。 */
    private boolean canFitAt(Vec3 pos) {
        AABB box = getDimensions(getPose()).makeBoundingBox(pos);
        return level().noBlockCollision(this, box);
    }

    /** 潜伏点在方块内部的落点；实体高度为 0.5，取方块中心避免探出。 */
    public static Vec3 burrowPos(BlockPos anchor) {
        return new Vec3(anchor.getX() + 0.5D, anchor.getY() + 0.25D, anchor.getZ() + 0.5D);
    }

    /** 钻出后站在幽匿块顶面的落点。 */
    public static Vec3 emergencePos(BlockPos anchor) {
        return new Vec3(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D);
    }

    /** 从 anchor 侧面钻出的落点：同一层外侧那一格的地面。 */
    public static Vec3 sideEmergePos(BlockPos anchor, Direction direction) {
        return new Vec3(anchor.getX() + 0.5D + direction.getStepX(), anchor.getY(),
                anchor.getZ() + 0.5D + direction.getStepZ());
    }

    /** 判断一个方块是否是完整实心方块（碰撞箱占满整格），用于判断能不能落脚。 */
    public static boolean isSolidBlock(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    /** 判断一个方块是否是完整的幽匿系方块，避免把没有碰撞体积的幽匿脉络当作潜伏点。 */
    public static boolean isFullSculkBlock(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return SculkMob.isSculkBlock(state) && state.isCollisionShapeFullBlock(level, pos);
    }

    /** 在 origin 附近寻找最近的完整幽匿系方块，找不到返回 {@code null}。 */
    @Nullable
    public static BlockPos findSculkAnchor(LevelReader level, BlockPos origin, int horizontalRadius, int verticalRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!isFullSculkBlock(level, cursor)) continue;

                    double distance = origin.distSqr(cursor);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }

        return best;
    }

    private void moveToBurrowCenter(BlockPos anchor) {
        Vec3 pos = burrowPos(anchor);
        moveTo(pos.x, pos.y, pos.z, getYRot(), getXRot());
    }

    /**
     * 压低环境音音量。
     *
     * <p>只改 ambient 这一条：原版蠹虫按 {@code getSoundVolume()}（1.0）播放环境音，
     * 这里固定为 0.1，受伤、死亡、脚步、攻击等其它声音保持原样。
     */
    @Override
    public void playAmbientSound() {
      if (getBurrowState().equals(BurrowState.BURROWED)) playSound(getAmbientSound(), 0.02F, getVoicePitch());
      else super.playAmbientSound();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 姿势完全由客户端的 SculverfishCemAnimator 计算（CEM 公式本身包含待机、行走、受伤与死亡），
        // 因此不注册关键帧动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
