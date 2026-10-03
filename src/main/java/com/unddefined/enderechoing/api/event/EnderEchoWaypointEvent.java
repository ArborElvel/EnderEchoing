package com.unddefined.enderechoing.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 末影回响路径点相关事件。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link Pre}、{@link Added}、{@link Removed} 与 {@link Modified}。
 * 事件派发在 {@code NeoForge.EVENT_BUS} 上。
 */
public abstract class EnderEchoWaypointEvent extends Event {
    private final ServerPlayer owner;
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final String name;
    private final int iconIndex;
    private final boolean anchorBound;

    protected EnderEchoWaypointEvent(ServerPlayer owner, ResourceKey<Level> dimension, BlockPos pos,
                                    String name, int iconIndex, boolean anchorBound) {
        this.owner = owner;
        this.dimension = dimension;
        this.pos = pos;
        this.name = name;
        this.iconIndex = iconIndex;
        this.anchorBound = anchorBound;
    }

    /** 路径点所属的玩家，即它会出现在谁的路径点列表里。 */
    public ServerPlayer getOwner() {
        return owner;
    }

    /** 路径点所在维度。 */
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    /** 路径点坐标。 */
    public BlockPos getPos() {
        return pos;
    }

    /** 路径点名称。死亡标记的名称已经过翻译并带 ☠ 前缀。 */
    public String getName() {
        return name;
    }

    /** 图标索引，0 为默认图标。 */
    public int getIconIndex() {
        return iconIndex;
    }

    /** 该点是否绑定在末影回响锚点上。 */
    public boolean isAnchorBound() {
        return anchorBound;
    }

    /**
     * 路径点写入某个玩家的列表后派发，不可取消。
     *
     * <p>队伍分享时接收方每人各派发一次，{@link #getOwner()} 是接收方而不是分享者。
     * 同一坐标已有路径点时不会派发本事件，改写请见 {@link Modified}。
     */
    public static final class Added extends EnderEchoWaypointEvent {
        public Added(ServerPlayer owner, ResourceKey<Level> dimension, BlockPos pos,
                     String name, int iconIndex, boolean anchorBound) {
            super(owner, dimension, pos, name, iconIndex, anchorBound);
        }
    }

    /**
     * 路径点从某个玩家的列表移除后派发，不可取消。删除不存在的点不会派发。
     *
     * <p>客户端编辑后整体同步回来时，只有相对旧列表真正消失的点会派发；
     * 只是顺序变化不会派发。
     */
    public static final class Removed extends EnderEchoWaypointEvent {
        public Removed(ServerPlayer owner, ResourceKey<Level> dimension, BlockPos pos,
                       String name, int iconIndex, boolean anchorBound) {
            super(owner, dimension, pos, name, iconIndex, anchorBound);
        }
    }

    /**
     * 同一坐标的路径点被改写（名称 / 图标 / 是否绑定锚点发生变化）后派发，不可取消。
     *
     * <p>事件自身携带改写后的值，改写前的值见 {@link #getPrevious()}。
     * 只是顺序变化不会派发；坐标被换掉会按 {@link Removed} 加 {@link Added} 处理。
     */
    public static final class Modified extends EnderEchoWaypointEvent {
        private final Previous previous;

        public Modified(ServerPlayer owner, ResourceKey<Level> dimension, BlockPos pos,
                        Previous previous, String name, int iconIndex, boolean anchorBound) {
            super(owner, dimension, pos, name, iconIndex, anchorBound);
            this.previous = previous;
        }

        /** 改写前的名称、图标与锚点绑定状态。 */
        public Previous getPrevious() {
            return previous;
        }
    }

    /**
     * 路径点即将写入某个玩家的列表前派发，可取消。
     *
     * <p>取消后不会写入路径点，也不会派发 {@link Added}。各条路径的处理：
     * 珍珠标记与死亡标记不再扣经验或珍珠；调谐器、调谐腔插入的珍珠照常兑换成计数，
     * 只是不写路径点；客户端调谐界面整体同步时，被拦截的新增点会被丢弃；
     * 队伍分享时该接收方跳过，不派发分享事件也不收费。同坐标覆盖已有路径点同样经过本事件。
     */
    public static final class Pre extends EnderEchoWaypointEvent implements ICancellableEvent {
        public Pre(ServerPlayer owner, ResourceKey<Level> dimension, BlockPos pos,
                   String name, int iconIndex, boolean anchorBound) {
            super(owner, dimension, pos, name, iconIndex, anchorBound);
        }
    }

    /**
     * 路径点改写前的值。
     */
    public record Previous(String name, int iconIndex, boolean anchorBound) {
    }
}
