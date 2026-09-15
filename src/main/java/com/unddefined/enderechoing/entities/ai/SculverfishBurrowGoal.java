package com.unddefined.enderechoing.entities.ai;

import com.unddefined.enderechoing.entities.SculverfishEntity;
import com.unddefined.enderechoing.entities.SculverfishEntity.BurrowState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 幽匿蠹虫的潜伏、钻出和钻回 Goal。
 *
 * <p>该 Goal 在 {@link BurrowState#ACTIVE} 之外占用 {@code MOVE / JUMP / TARGET} 标记，
 * 因此潜伏和钻地期间会压住原版近战、索敌与游荡 Goal；钻出完成后释放标记，
 * 交由 {@link net.minecraft.world.entity.monster.Silverfish} 体系保留的普通战斗 Goal 处理。
 * 在非幽匿方块上触发钻地时，它会用同样的标记接管移动，先走到附近最近的幽匿块再钻进去。
 *
 * <p>地下移动只经过 {@link SculverfishEntity#isFullSculkBlock} 认定的完整幽匿块，
 * 到达节点后再把该方块设为锚点并刷新限制范围，从而保证它不会长期离开幽匿区域。
 */
public class SculverfishBurrowGoal extends Goal {
    /** 两次地下换位之间的间隔范围（tick）。 */
    private static final int MIN_RELOCATE_INTERVAL = 40;
    private static final int MAX_RELOCATE_INTERVAL = 100;

    /** 一次地下随机游走的最大步数（格）。 */
    private static final int MAX_RELOCATE_STEPS = 5;

    /** 地下移动时每 tick 的速度（格）。 */
    private static final double BURROW_MOVE_SPEED = 0.12D;

    /** 钻出/钻入时每 tick 的速度（格）。 */
    private static final double TRANSITION_SPEED = 0.18D;

    /** 钻出/钻入最多持续的时间（tick），防止被卡住。 */
    private static final int MAX_TRANSITION_TICKS = 12;

    /** 受伤钻地后至少保持潜伏的时间（tick），期间只在地下换位，不会马上钻出。 */
    private static final int HURT_HIDE_TICKS = 60;

    /** 钻出/钻入一次的方块碎屑粒子数量与水平散布（格），对齐原版监守者挖地的表现。 */
    private static final int DIG_PARTICLE_COUNT = 10;
    private static final double DIG_PARTICLE_SPREAD = 0.3D;

    /** 侧面碎屑：每个面的粒子数量，以及为了让碎屑露在方块外而沿面法线往外偏的距离（格）。 */
    private static final int DIG_SIDE_PARTICLE_COUNT = 6;
    private static final double DIG_SIDE_OFFSET = 0.02D;

    /** 钻入/钻出允许的最大位移（格）的平方；目标必须就在脚下或紧邻，避免“远程钻进”方块。 */
    private static final double MAX_TRANSITION_DISTANCE_SQR = 2.25D;

    /** 暴露在外（地面活动）时，向外搜索可钻幽匿块的最大水平/竖直半径（格）。 */
    private static final int ANCHOR_SEARCH_RADIUS = 16;
    private static final int ANCHOR_SEARCH_HEIGHT = 8;

    /** 走过去钻时的移动速度，以及最长接近时间（tick）；超时或走不到就放弃接近。 */
    private static final double APPROACH_SPEED = 1.0D;
    private static final int MAX_APPROACH_TICKS = 200;

    /** 接近途中的重新寻路间隔（tick）与连续寻路失败多少次后放弃。 */
    private static final int MIN_REPATH_INTERVAL = 10;
    private static final int MAX_REPATH_INTERVAL = 20;
    private static final int MAX_REPATH_FAILURES = 3;

    private final SculverfishEntity mob;
    private final List<BlockPos> relocatePath = new ArrayList<>();

    private int relocateCooldown;
    private int pathIndex;
    private int transitionTicks;
    private int hideTicks;

    /** 本次钻地是否由受伤触发；受伤钻地在钻到底后要先躲一段时间。 */
    private boolean hurtDive;

    /** 附近找到的幽匿块；不为空时处于地面接近阶段，走到它旁边再钻进去。 */
    @Nullable
    private BlockPos approachTarget;
    private int approachTicks;
    private int repathCooldown;
    private int repathFailures;

    @Nullable
    private BlockPos transitionTarget;
    @Nullable
    private Player emergeTarget;

    public SculverfishBurrowGoal(SculverfishEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive()) return false;
        if (mob.getBurrowState() == BurrowState.ACTIVE) {
            // 受伤、目标丢失或地面停留超时后，主动接管战斗 Goal 开始钻地
            return mob.shouldBurrow();
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive()) return false;
        // 地面接近阶段也归这个 Goal 管：回到幽匿块钻进去（或放弃）之前不交还控制权
        return mob.getBurrowState() != BurrowState.ACTIVE || approachTarget != null;
    }

    @Override
    public void start() {
        if (mob.getBurrowState() == BurrowState.ACTIVE) {
            // 受伤钻地的请求要在 refreshBurrowState 清掉它之前取出来
            this.hurtDive = mob.consumeBurrowRequest();
        }
        mob.refreshBurrowState();
        mob.getNavigation().stop();

        if (mob.getBurrowState() == BurrowState.ACTIVE) {
            if (findBurrowDestination() != null) {
                mob.setBurrowState(BurrowState.BURROWING);
            } else {
                // 脚下/紧邻没有幽匿块：先在附近搜一个能走过去的，走过去再钻
                this.approachTarget = findApproachTarget();
                this.approachTicks = 0;
                this.repathCooldown = 0;
                this.repathFailures = 0;
            }
        }

        switch (mob.getBurrowState()) {
            case BURROWED -> {
                relocatePath.clear();
                pathIndex = 0;
                relocateCooldown = nextRelocateCooldown();
            }
            case BURROWING -> {
                transitionTicks = 0;
                transitionTarget = findBurrowDestination();
            }
            case EMERGING -> {
                transitionTicks = 0;
                transitionTarget = mob.getBurrowAnchor();
            }
            case ACTIVE -> {
            }
        }
    }

    @Override
    public void tick() {
        switch (mob.getBurrowState()) {
            case BURROWED -> tickBurrowed();
            case EMERGING -> tickEmerging();
            case BURROWING -> tickBurrowing();
            case ACTIVE -> tickApproaching();
        }
    }

    @Override
    public void stop() {
        relocatePath.clear();
        pathIndex = 0;
        transitionTarget = null;
        emergeTarget = null;
        approachTarget = null;
        approachTicks = 0;
        repathCooldown = 0;
        repathFailures = 0;
        hurtDive = false;
        hideTicks = 0;
        mob.getNavigation().stop();
    }

    private void tickBurrowed() {
        // 潜伏期间不接受爆炸/水流等外部位移，始终停留在锚点方块内部
        mob.setDeltaMovement(Vec3.ZERO);

        BlockPos anchor = mob.getBurrowAnchor();
        if (anchor == null || !SculverfishEntity.isFullSculkBlock(mob.level(), anchor)) {
            // 类似被挖掉的虫蚀方块：优先当场钻出暴露自己，出口被堵死时才换到紧邻的一格
            if (tryEmergeFromCurrentBlock()) return;

            BlockPos replacement = findBurrowDestination();
            if (replacement != null) {
                mob.setBurrowAnchor(replacement);
                beginBurrowing(replacement);
            } else {
                // 潜伏点消失且附近没有幽匿块时只能回到地面，避免卡在非幽匿方块里
                mob.setBurrowState(BurrowState.ACTIVE);
            }
            return;
        }

        if (mob.consumeBurrowRequest()) {
            // 受伤时优先钻地：先在地下换位并躲一段时间，避免钻回去又被同一个目标打出来
            hideTicks = HURT_HIDE_TICKS;
            relocateCooldown = 0;
        }

        if (hideTicks > 0) {
            hideTicks--;
        } else {
            Player player = mob.findNearbyPlayer();
            if (player != null) {
                Vec3 emergePos = mob.findEmergePos(anchor);
                if (emergePos != null && mob.canSeeFrom(emergePos, player)) {
                    beginEmerging(player);
                    return;
                }
                // 附近有玩家但上下左右都钻不出去：优先换一个能钻出的位置
                if (emergePos == null) relocateCooldown = 0;
            }
        }

        if (!relocatePath.isEmpty()) {
            followRelocatePath();
            return;
        }

        if (--relocateCooldown <= 0 && !buildRelocatePath(anchor)) {
            relocateCooldown = 20;
        }
    }

    private void tickEmerging() {
        BlockPos anchor = transitionTarget != null ? transitionTarget : mob.getBurrowAnchor();
        Vec3 target = anchor != null ? mob.findEmergePos(anchor) : null;
        if (target == null) {
            // 钻出过程中出口被堵住，退回潜伏状态
            mob.setBurrowState(BurrowState.BURROWED);
            return;
        }

        transitionTicks++;
        if (transitionTicks >= MAX_TRANSITION_TICKS || mob.position().distanceToSqr(target) < 0.01D) {
            mob.moveTo(target.x, target.y, target.z, mob.getYRot(), mob.getXRot());
            mob.setBurrowState(BurrowState.ACTIVE);
            if (emergeTarget != null && emergeTarget.isAlive()) mob.setTarget(emergeTarget);

            return;
        }

        moveTowards(target, TRANSITION_SPEED);
    }

    private void tickBurrowing() {
        if (!isValidBurrowTarget(transitionTarget)) {
            transitionTarget = findBurrowDestination();
            transitionTicks = 0;
            if (!isValidBurrowTarget(transitionTarget)) {
                transitionTarget = null;
                mob.setBurrowState(BurrowState.ACTIVE);
                return;
            }
        }

        transitionTicks++;
        Vec3 target = SculverfishEntity.burrowPos(transitionTarget);
        if (transitionTicks >= MAX_TRANSITION_TICKS || mob.position().distanceToSqr(target) < 0.01D) {
            mob.moveTo(target.x, target.y, target.z, mob.getYRot(), mob.getXRot());
            mob.setBurrowAnchor(transitionTarget);
            mob.setBurrowState(BurrowState.BURROWED);
            playBurrowEffects(transitionTarget, false);
            relocatePath.clear();
            pathIndex = 0;
            relocateCooldown = nextRelocateCooldown();
            if (hurtDive) {
                // 受伤钻地：先在地下躲一段时间并换位，再考虑钻出
                hurtDive = false;
                hideTicks = HURT_HIDE_TICKS;
                relocateCooldown = 0;
            }
            return;
        }

        moveTowards(target, TRANSITION_SPEED);
    }

    /**
     * 地面接近阶段：用寻路走到附近找到的那格幽匿块上面，路上踩到幽匿块就提前钻进去。
     *
     * <p>钻入本身仍然是原地短过渡，这里刻意让它自己走过去，避免出现“远程钻进方块”。
     */
    private void tickApproaching() {
        BlockPos target = approachTarget;
        if (target == null) return;

        BlockPos destination = findBurrowDestination();
        if (destination != null) {
            approachTarget = null;
            beginBurrowing(destination);
            return;
        }

        if (++approachTicks > MAX_APPROACH_TICKS) {
            cancelApproach();
            return;
        }

        mob.getLookControl().setLookAt(Vec3.atBottomCenterOf(target));

        if (--repathCooldown > 0) return;

        repathCooldown = MIN_REPATH_INTERVAL + mob.getRandom().nextInt(MAX_REPATH_INTERVAL - MIN_REPATH_INTERVAL + 1);
        boolean pathed = mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                APPROACH_SPEED);
        if (pathed) {
            repathFailures = 0;
        } else if (++repathFailures >= MAX_REPATH_FAILURES) {
            // 走不到（被挡住、目标在限制范围外等）：放弃接近，把控制权还给战斗 Goal
            cancelApproach();
        }
    }

    /** 放弃接近并停下导航。 */
    private void cancelApproach() {
        approachTarget = null;
        mob.getNavigation().stop();
    }

    /** 开始钻出：记录目标并播放钻出表现。 */
    private void beginEmerging(Player player) {
        BlockPos anchor = mob.getBurrowAnchor();
        if (anchor == null) return;

        this.emergeTarget = player;
        this.transitionTarget = anchor;
        this.transitionTicks = 0;
        this.relocatePath.clear();
        this.pathIndex = 0;
        mob.setBurrowState(BurrowState.EMERGING);
        playBurrowEffects(anchor, true);
    }

    /** 开始钻入指定幽匿块。 */
    private void beginBurrowing(BlockPos target) {
        this.transitionTarget = target;
        this.transitionTicks = 0;
        this.relocatePath.clear();
        this.pathIndex = 0;
        mob.setBurrowState(BurrowState.BURROWING);
    }

    /**
     * 只能钻入自身所在、或紧邻（脚下、同级相邻、斜下方、正上方）的一格完整幽匿块。
     *
     * <p>它可以在垂直方向的幽匿块里上下移动，也能从侧面钻进贴着的幽匿块。
     * <p>不搜索更远的目标：钻入是一次短距离的过渡动画，跨格目标会变成从远处“飞进”幽匿块。
     */
    @Nullable
    private BlockPos findBurrowDestination() {
        BlockPos current = mob.blockPosition();
        if (SculverfishEntity.isFullSculkBlock(mob.level(), current)) return current;

        BlockPos below = current.below();
        if (SculverfishEntity.isFullSculkBlock(mob.level(), below)) return below;

        BlockPos above = current.above();
        if (SculverfishEntity.isFullSculkBlock(mob.level(), above)) return above;

        // 同级相邻的一格，以及刚走下来那一层（斜下方一格，即脚边地面的幽匿块）
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = current.relative(direction);
            if (SculverfishEntity.isFullSculkBlock(mob.level(), side)) return side;

            BlockPos sideBelow = side.below();
            if (SculverfishEntity.isFullSculkBlock(mob.level(), sideBelow)) return sideBelow;
        }

        return null;
    }

    /** 钻地目标是否仍然可钻：存在、是完整幽匿块，并且就在脚下或紧邻。 */
    private boolean isValidBurrowTarget(@Nullable BlockPos target) {
        if (target == null || !SculverfishEntity.isFullSculkBlock(mob.level(), target)) return false;
        return mob.position().distanceToSqr(SculverfishEntity.burrowPos(target)) <= MAX_TRANSITION_DISTANCE_SQR;
    }

    /**
     * 在水平 {@value #ANCHOR_SEARCH_RADIUS} 格、竖直 {@value #ANCHOR_SEARCH_HEIGHT} 格内
     * 找最近的、能钻进去的幽匿块旁的落脚点，作为地面接近阶段的目标。
     *
     * <p>从近到远逐层向外搜索，某一层里找到就直接返回，因此范围开得大也不会每次都扫满全盒。
     * 只用于“走过去再钻”：找到的方块不会直接成为钻入目标，钻入永远发生在脚边或紧邻一格。
     */
    @Nullable
    private BlockPos findApproachTarget() {
        BlockPos origin = mob.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int radius = 1; radius <= ANCHOR_SEARCH_RADIUS; radius++) {
            int verticalLimit = Math.min(radius, ANCHOR_SEARCH_HEIGHT);
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int y = -verticalLimit; y <= verticalLimit; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        // 只看这一层的壳，里层已经确认没有可钻的幽匿块了
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;

                        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        if (!isApproachStand(cursor)) continue;

                        double distance = origin.distSqr(cursor);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = cursor.immutable();
                        }
                    }
                }
            }

            if (best != null) return best;
        }

        return null;
    }

    /**
     * 这一格能不能作为接近幽匿块的落脚点。
     *
     * <p>判据与钻出、钻入保持一致：本身不能是实心方块（容得下它），下面必须是实心方块
     * （钻出来有地方站），上方可以不是；站上去之后，脚下、正上方、四邻或斜下方任意一处
     * 是完整幽匿块就算能钻进去。
     */
    private boolean isApproachStand(BlockPos pos) {
        if (SculverfishEntity.isSolidBlock(mob.level(), pos)) return false;
        if (!SculverfishEntity.isSolidBlock(mob.level(), pos.below())) return false;
        if (SculverfishEntity.isFullSculkBlock(mob.level(), pos.below())) return true;
        if (SculverfishEntity.isFullSculkBlock(mob.level(), pos.above())) return true;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (SculverfishEntity.isFullSculkBlock(mob.level(), side)) return true;
            if (SculverfishEntity.isFullSculkBlock(mob.level(), side.below())) return true;
        }

        return false;
    }

    /** 潜伏方块被破坏/替换时，如果上方有空间就当场钻出。 */
    private boolean tryEmergeFromCurrentBlock() {
        BlockPos pos = mob.blockPosition();
        Vec3 surface = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        AABB box = mob.getDimensions(mob.getPose()).makeBoundingBox(surface);
        if (!mob.level().noBlockCollision(mob, box)) return false;

        mob.moveTo(surface.x, surface.y, surface.z, mob.getYRot(), mob.getXRot());
        mob.setBurrowState(BurrowState.ACTIVE);
        playBurrowEffects(pos, true);
        return true;
    }

    /** 生成一条只经过完整幽匿方块的随机游走路径，并优先选择能钻出的终点。 */
    private boolean buildRelocatePath(BlockPos start) {
        relocatePath.clear();
        pathIndex = 0;

        List<BlockPos> fallback = List.of();
        for (int attempt = 0; attempt < 4; attempt++) {
            List<BlockPos> candidate = randomSculkPath(start,
                    2 + mob.getRandom().nextInt(MAX_RELOCATE_STEPS - 1));
            if (candidate.isEmpty()) continue;

            if (mob.findEmergePos(candidate.get(candidate.size() - 1)) != null) {
                relocatePath.addAll(candidate);
                return true;
            }

            if (candidate.size() > fallback.size()) fallback = new ArrayList<>(candidate);
        }

        relocatePath.addAll(fallback);
        return !relocatePath.isEmpty();
    }

    private List<BlockPos> randomSculkPath(BlockPos start, int steps) {
        List<BlockPos> path = new ArrayList<>(steps);
        BlockPos current = start;
        BlockPos previous = null;

        for (int i = 0; i < steps; i++) {
            List<BlockPos> neighbors = new ArrayList<>(6);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (next.equals(previous)) continue;
                if (SculverfishEntity.isFullSculkBlock(mob.level(), next)) neighbors.add(next);
            }

            if (neighbors.isEmpty()) break;

            BlockPos next = neighbors.get(mob.getRandom().nextInt(neighbors.size()));
            path.add(next);
            previous = current;
            current = next;
        }

        return path;
    }

    private void followRelocatePath() {
        if (pathIndex < 0 || pathIndex >= relocatePath.size()) {
            relocatePath.clear();
            pathIndex = 0;
            return;
        }

        BlockPos node = relocatePath.get(pathIndex);
        if (!SculverfishEntity.isFullSculkBlock(mob.level(), node)) {
            relocatePath.clear();
            pathIndex = 0;
            relocateCooldown = 20;
            return;
        }

        Vec3 target = SculverfishEntity.burrowPos(node);
        if (mob.position().distanceToSqr(target) < 0.04D) {
            mob.setBurrowAnchor(node);
            pathIndex++;
            if (pathIndex >= relocatePath.size()) {
                relocatePath.clear();
                pathIndex = 0;
                relocateCooldown = nextRelocateCooldown();
            }
            return;
        }

        moveTowards(target, BURROW_MOVE_SPEED);
    }

    private void moveTowards(Vec3 target, double speed) {
        Vec3 delta = target.subtract(mob.position());
        if (delta.lengthSqr() < 1.0E-6D) return;

        mob.getLookControl().setLookAt(target);
        mob.move(MoverType.SELF, delta.normalize().scale(speed));
        mob.setDeltaMovement(Vec3.ZERO);
    }

    private int nextRelocateCooldown() {
        return MIN_RELOCATE_INTERVAL + mob.getRandom().nextInt(MAX_RELOCATE_INTERVAL - MIN_RELOCATE_INTERVAL + 1);
    }

    /** 钻出/钻入时播放被挖开方块的碎屑粒子与幽匿音效。 */
    private void playBurrowEffects(BlockPos pos, boolean emerging) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        // 与原版监守者一致：用被挖开方块的碎屑粒子
        BlockState state = serverLevel.getBlockState(pos);
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, state);

            // 顶面：它钻入/钻出的位置
            serverLevel.sendParticles(debris,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    DIG_PARTICLE_COUNT, DIG_PARTICLE_SPREAD, 0.0D, DIG_PARTICLE_SPREAD, 0.0D);

            // 四个侧面：碎屑贴在面外侧、沿该面所在平面散开，让侧面也看得到方块剥落
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                double x = pos.getX() + 0.5D + direction.getStepX() * (0.5D + DIG_SIDE_OFFSET);
                double z = pos.getZ() + 0.5D + direction.getStepZ() * (0.5D + DIG_SIDE_OFFSET);
                serverLevel.sendParticles(debris, x, pos.getY() + 0.5D, z, DIG_SIDE_PARTICLE_COUNT,
                        direction.getStepX() == 0 ? DIG_PARTICLE_SPREAD : 0.0D, DIG_PARTICLE_SPREAD,
                        direction.getStepZ() == 0 ? DIG_PARTICLE_SPREAD : 0.0D, 0.0D);
            }
        }

        serverLevel.playSound(null, pos,
                emerging ? SoundEvents.SCULK_BLOCK_BREAK : SoundEvents.SCULK_BLOCK_PLACE,
                SoundSource.BLOCKS, 0.45F, 0.8F + mob.getRandom().nextFloat() * 0.4F);
    }
}
