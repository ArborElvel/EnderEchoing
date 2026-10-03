package com.unddefined.enderechoing.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 末影回响锚点相关事件。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link Pre}、{@link Added} 与 {@link Removed}。
 * 事件派发在 {@code NeoForge.EVENT_BUS} 上。
 *
 * <p>这里说的锚点就是玩家数据里的 {@code anchors}，与路径点
 * （{@link EnderEchoWaypointEvent}）不是一回事。只有该玩家的锚点列表真的发生变化时才派发事件。
 */
public abstract class EnderEchoAnchorEvent extends Event {
    private final ServerPlayer owner;
    private final GlobalPos pos;

    protected EnderEchoAnchorEvent(ServerPlayer owner, GlobalPos pos) {
        this.owner = owner;
        this.pos = pos;
    }

    /** 锚点所属的玩家，即它出现在谁的锚点列表里。 */
    public ServerPlayer getOwner() {
        return owner;
    }

    /** 锚点坐标。 */
    public GlobalPos getPos() {
        return pos;
    }

    /** 锚点所在维度。 */
    public ResourceKey<Level> getDimension() {
        return pos.dimension();
    }

    /** 锚点所在方块位置。 */
    public BlockPos getBlockPos() {
        return pos.pos();
    }

    /**
     * 锚点登记进某个玩家的列表后派发，不可取消。重复登记不会派发。
     */
    public static final class Added extends EnderEchoAnchorEvent {
        public Added(ServerPlayer owner, GlobalPos pos) {
            super(owner, pos);
        }
    }

    /**
     * 锚点即将建立前派发，可取消。
     *
     * <p>取消后既不会登记锚点，也不会放置对应方块、不会消耗物品。
     * 通过珍珠或调谐腔做补登记的路径取消后只是不补登记，不影响其它流程。
     * 此时锚点还不存在，{@link #getOwner()} 是发起登记的玩家。
     */
    public static final class Pre extends EnderEchoAnchorEvent implements ICancellableEvent {
        public Pre(ServerPlayer owner, GlobalPos pos) {
            super(owner, pos);
        }
    }

    /**
     * 锚点从某个玩家的列表移除后派发，不可取消。
     *
     * <p>移除原因可能是锚点方块被破坏，或玩家登录时该点被判定为失效。
     */
    public static final class Removed extends EnderEchoAnchorEvent {
        public Removed(ServerPlayer owner, GlobalPos pos) {
            super(owner, pos);
        }
    }
}
