package com.unddefined.enderechoing.api.event;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 末影回响传送相关事件的基类。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link Pre} 与 {@link Post}。所有事件都派发在 {@code NeoForge.EVENT_BUS} 上。
 */
public abstract class EnderEchoTeleportEvent extends Event {
    private final ServerPlayer player;
    private final Level fromLevel;
    private final Vec3 fromPos;
    private final GlobalPos target;

    protected EnderEchoTeleportEvent(ServerPlayer player, Level fromLevel, Vec3 fromPos, GlobalPos target) {
        this.player = player;
        this.fromLevel = fromLevel;
        this.fromPos = fromPos;
        this.target = target;
    }

    /** 发起本次传送的玩家。 */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * 传送前所在维度。
     * 跨维度传送后它不等于 {@link #getPlayer()} 当前的维度。
     */
    public Level getFromLevel() {
        return fromLevel;
    }

    /** 传送前的位置，位于 {@link #getFromLevel()} 中。 */
    public Vec3 getFromPos() {
        return fromPos;
    }

    /** 本次传送的目标坐标。 */
    public GlobalPos getTarget() {
        return target;
    }

    /**
     * 传送执行前派发，可取消。
     *
     * <p>取消后本次传送不会发生，也不会扣除珍珠、消耗重生锚充能或进入冷却。
     * 与 {@link Post} 一样，一次传送动作只派发一次，且只针对发起传送的玩家。
     */
    public static final class Pre extends EnderEchoTeleportEvent implements ICancellableEvent {
        public Pre(ServerPlayer player, Level fromLevel, Vec3 fromPos, GlobalPos target) {
            super(player, fromLevel, fromPos, target);
        }
    }

    /**
     * 传送成功后派发，不可取消。
     *
     * <p>派发时玩家已经在目标位置。一次传送动作只派发一次，且只包含发起传送的玩家：
     * 由回响核心一同带走的同行玩家不会各自派发。事件目前不携带传送方式
     * （核心 / 谐振器 / 折跃平台 / 水晶），需要区分时只能结合上下文判断。
     */
    public static final class Post extends EnderEchoTeleportEvent {
        public Post(ServerPlayer player, Level fromLevel, Vec3 fromPos, GlobalPos target) {
            super(player, fromLevel, fromPos, target);
        }
    }
}
