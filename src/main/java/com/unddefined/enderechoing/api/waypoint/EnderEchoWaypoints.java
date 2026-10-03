package com.unddefined.enderechoing.api.waypoint;

import com.unddefined.enderechoing.api.event.EnderEchoWaypointEvent;
import com.unddefined.enderechoing.server.DataComponents.EnderEchoPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家名下的路径点：读写入口，模组内部也走这里。
 *
 * <p>路径点就是玩家数据里的 {@code waypoints}，与末影回响锚点不是一回事。
 * 写入、改写与移除都会派发 {@link EnderEchoWaypointEvent} 下的事件。
 */
public final class EnderEchoWaypoints {
    private EnderEchoWaypoints() {
    }

    /** 该玩家已登记的路径点快照。 */
    public static List<EnderEchoWaypoint> of(Player player) {
        return List.copyOf(EnderEchoPlayerData.getManager(player).waypoints());
    }

    /**
     * 派发可取消的 {@link EnderEchoWaypointEvent.Pre}，只判定不改数据；非服务端玩家一律放行。
     */
    public static boolean canAdd(Player player, ResourceKey<Level> dimension, BlockPos pos,
                                 String name, int iconIndex, boolean anchorBound) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        var event = new EnderEchoWaypointEvent.Pre(serverPlayer, dimension, pos, name, iconIndex, anchorBound);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    /**
     * 写入路径点，同坐标会覆盖旧点。
     *
     * <p>先派发可取消的 {@link EnderEchoWaypointEvent.Pre}，被拦截时返回 false 且不写入。
     * 新坐标派发 {@link EnderEchoWaypointEvent.Added}；同坐标内容有变化派发
     * {@link EnderEchoWaypointEvent.Modified}；内容完全相同时不派发。
     *
     * @return true 表示写入成功；参数不合法或被拦截时返回 false
     */
    public static boolean add(Player player, ResourceKey<Level> dimension, BlockPos pos,
                              String name, int iconIndex, boolean anchorBound) {
        if (pos == null || name == null || dimension == null) return false;
        if (!canAdd(player, dimension, pos, name, iconIndex, anchorBound)) return false;
        var data = EnderEchoPlayerData.getManager(player);
        var existing = sameCoord(data.waypoints(), dimension, pos);
        data.waypoints().removeIf(entry -> entry.dimension().equals(dimension) && entry.pos().equals(pos));
        var added = new EnderEchoWaypoint(dimension, pos, name, iconIndex, anchorBound);
        data.waypoints().add(added);
        if (player instanceof ServerPlayer serverPlayer) {
            if (existing == null) {
                NeoForge.EVENT_BUS.post(new EnderEchoWaypointEvent.Added(serverPlayer,
                        dimension, pos, name, iconIndex, anchorBound));
            } else if (!existing.equals(added)) {
                NeoForge.EVENT_BUS.post(new EnderEchoWaypointEvent.Modified(serverPlayer, dimension, pos,
                        previousOf(existing), name, iconIndex, anchorBound));
            }
        }
        return true;
    }

    /**
     * 删除指定坐标的路径点，并派发 {@link EnderEchoWaypointEvent.Removed}。
     *
     * @return true 表示确实删除了；该坐标没有路径点时返回 false，也不会派发事件
     */
    public static boolean remove(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        var data = EnderEchoPlayerData.getManager(player);
        var removed = sameCoord(data.waypoints(), dimension, pos);
        if (removed == null) return false;
        data.waypoints().remove(removed);
        if (player instanceof ServerPlayer serverPlayer)
            NeoForge.EVENT_BUS.post(new EnderEchoWaypointEvent.Removed(serverPlayer,
                    removed.dimension(), removed.pos(), removed.name(), removed.iconIndex(), removed.anchorBound()));
        return true;
    }

    /**
     * 用同步来的列表整体替换路径点，并按新旧差异派发 {@link EnderEchoWaypointEvent.Added} /
     * {@link EnderEchoWaypointEvent.Removed} 与 {@link EnderEchoWaypointEvent.Modified}。
     *
     * <p>新增的点要先通过 {@link #canAdd}，被拦截的直接丢弃；纯顺序变化不会派发事件。
     */
    public static void replace(Player player, List<EnderEchoWaypoint> next) {
        var data = EnderEchoPlayerData.getManager(player);
        var previous = List.copyOf(data.waypoints());
        var accepted = new ArrayList<EnderEchoWaypoint>(next.size());
        for (EnderEchoWaypoint now : next) {
            if (sameCoord(previous, now.dimension(), now.pos()) == null
                    && !canAdd(player, now.dimension(), now.pos(), now.name(), now.iconIndex(), now.anchorBound()))
                continue;
            accepted.add(now);
        }
        data.waypoints().clear();
        data.waypoints().addAll(accepted);
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        for (EnderEchoWaypoint old : previous) {
            var now = sameCoord(accepted, old.dimension(), old.pos());
            if (now == null) {
                NeoForge.EVENT_BUS.post(new EnderEchoWaypointEvent.Removed(serverPlayer,
                        old.dimension(), old.pos(), old.name(), old.iconIndex(), old.anchorBound()));
            } else if (!now.equals(old)) {
                NeoForge.EVENT_BUS.post(new EnderEchoWaypointEvent.Modified(serverPlayer,
                        old.dimension(), old.pos(), previousOf(old),
                        now.name(), now.iconIndex(), now.anchorBound()));
            }
        }
        for (EnderEchoWaypoint now : accepted) {
            if (sameCoord(previous, now.dimension(), now.pos()) != null) continue;
            NeoForge.EVENT_BUS.post(new EnderEchoWaypointEvent.Added(serverPlayer,
                    now.dimension(), now.pos(), now.name(), now.iconIndex(), now.anchorBound()));
        }
    }

    private static EnderEchoWaypointEvent.Previous previousOf(EnderEchoWaypoint waypoint) {
        return new EnderEchoWaypointEvent.Previous(waypoint.name(), waypoint.iconIndex(), waypoint.anchorBound());
    }

    @Nullable
    private static EnderEchoWaypoint sameCoord(List<EnderEchoWaypoint> list,
                                               ResourceKey<Level> dimension, BlockPos pos) {
        for (EnderEchoWaypoint entry : list) {
            if (entry.dimension().equals(dimension) && entry.pos().equals(pos)) return entry;
        }
        return null;
    }
}
