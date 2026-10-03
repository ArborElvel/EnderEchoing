package com.unddefined.enderechoing.api.anchor;

import com.unddefined.enderechoing.api.event.EnderEchoAnchorEvent;
import com.unddefined.enderechoing.server.DataComponents.EnderEchoPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家名下的末影回响锚点：读写入口，模组内部也走这里。
 *
 * <p>锚点就是玩家数据里的 {@code anchors}，与路径点不是一回事。「某个位置算不算锚点」
 * 由 {@link EnderEchoAnchorRegistry} 里注册的判定器决定，本类管的是「登记到某个玩家名下」。
 */
public final class EnderEchoAnchors {
    private EnderEchoAnchors() {
    }

    /** 该玩家已登记的锚点快照。 */
    public static List<GlobalPos> of(Player player) {
        var data = EnderEchoPlayerData.getManager(player);
        var result = new ArrayList<GlobalPos>(data.anchors().size());
        for (var anchor : data.anchors()) result.add(anchor.globalPos());
        return result;
    }

    /** 该位置是否被已注册的判定器认可为末影回响锚点。 */
    public static boolean isAnchor(Level level, BlockPos pos) {
        return EnderEchoAnchorRegistry.isAnchor(level, pos);
    }

    /** 该位置是锚点，但还没有登记到该玩家名下。 */
    public static boolean isUnregisteredAnchor(Player player, Level level, BlockPos pos) {
        if (!isAnchor(level, pos)) return false;
        for (var anchor : EnderEchoPlayerData.getManager(player).anchors()) {
            if (anchor.dimension().equals(level.dimension()) && anchor.pos().equals(pos)) return false;
        }
        return true;
    }

    /**
     * 派发可取消的 {@link EnderEchoAnchorEvent.Pre}，只判定不改数据。
     *
     * <p>通过后调用方应继续放置方块（如果有）再调用 {@link #add}；非服务端玩家一律放行。
     */
    public static boolean canRegister(Player player, Level level, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        var event = new EnderEchoAnchorEvent.Pre(serverPlayer, GlobalPos.of(level.dimension(), pos));
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    /**
     * 把锚点登记到该玩家名下，并派发 {@link EnderEchoAnchorEvent.Added}。
     *
     * @return true 表示确实新增了锚点；重复登记返回 false，也不会派发事件
     */
    public static boolean add(Player player, Level level, BlockPos pos) {
        var data = EnderEchoPlayerData.getManager(player);
        var anchor = new EnderEchoPlayerData.Anchor(new GlobalPos(level.dimension(), pos));
        if (data.anchors().contains(anchor)) return false;
        data.anchors().add(anchor);
        data.checkBounds();
        if (player instanceof ServerPlayer serverPlayer)
            NeoForge.EVENT_BUS.post(new EnderEchoAnchorEvent.Added(serverPlayer, anchor.globalPos()));
        return true;
    }

    /**
     * 从该玩家名下移除锚点，并派发 {@link EnderEchoAnchorEvent.Removed}。
     *
     * @return true 表示确实移除了锚点；该点不存在时返回 false，也不会派发事件
     */
    public static boolean remove(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        var data = EnderEchoPlayerData.getManager(player);
        var anchor = new EnderEchoPlayerData.Anchor(new GlobalPos(dimension, pos));
        if (!data.anchors().remove(anchor)) return false;
        data.checkBounds();
        if (player instanceof ServerPlayer serverPlayer)
            NeoForge.EVENT_BUS.post(new EnderEchoAnchorEvent.Removed(serverPlayer, anchor.globalPos()));
        return true;
    }

    /** 该维度里离 {@code fromPos} 最近的锚点，没有则返回 {@code null}。 */
    @Nullable
    public static GlobalPos nearest(Player player, Level level, BlockPos fromPos) {
        GlobalPos nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;
        for (var anchor : EnderEchoPlayerData.getManager(player).anchors()) {
            if (!anchor.dimension().equals(level.dimension())) continue;
            double distance = anchor.pos().distSqr(fromPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPos = anchor.globalPos();
            }
        }
        return nearestPos;
    }

    /** 该玩家在给定维度已登记的锚点坐标。 */
    public static List<BlockPos> positions(Player player, Level level) {
        var result = new ArrayList<BlockPos>();
        for (var anchor : EnderEchoPlayerData.getManager(player).anchors()) {
            if (anchor.dimension().equals(level.dimension())) result.add(anchor.pos());
        }
        return result;
    }

    /** 该维度里绑定在锚点上的路径点：坐标 → 名称。 */
    public static Map<BlockPos, String> boundWaypoints(Player player, Level level) {
        var data = EnderEchoPlayerData.getManager(player);
        data.checkBounds();
        Map<BlockPos, String> result = new HashMap<>();
        for (var waypoint : data.waypoints()) {
            if (waypoint.dimension().equals(level.dimension()) && waypoint.anchorBound())
                result.put(waypoint.pos(), waypoint.name());
        }
        return result;
    }
}
