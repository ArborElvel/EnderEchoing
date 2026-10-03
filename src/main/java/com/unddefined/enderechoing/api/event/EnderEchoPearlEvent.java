package com.unddefined.enderechoing.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * 末影回响珍珠事件。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link Changed}。事件派发在 {@code NeoForge.EVENT_BUS} 上。
 *
 * <p>数量是玩家附件里的计数，允许为负数（死亡标记会扣到负数），事件不会做钳制。
 */
public abstract class EnderEchoPearlEvent extends Event {
    /**
     * 数量变化的原因。
     */
    public enum Cause {
        /** 传送扣费：水晶定向传送、回响核心、传送请求。 */
        TELEPORT,
        /** 用珍珠标记路径点。 */
        WAYPOINT,
        /** 珍珠计数与珍珠物品互相兑换：调谐器、调谐腔、把计数换回物品。 */
        CONVERT,
        /** 队伍分享时接收方支付。 */
        SHARE,
        /** 死亡时写入死亡标记。 */
        DEATH,
        /** 客户端调谐界面整体同步覆盖。 */
        SYNC
    }

    private final ServerPlayer player;
    private final int previousAmount;
    private final int amount;
    private final Cause cause;

    protected EnderEchoPearlEvent(ServerPlayer player, int previousAmount, int amount, Cause cause) {
        this.player = player;
        this.previousAmount = previousAmount;
        this.amount = amount;
        this.cause = cause;
    }

    /** 珍珠数量发生变化的玩家。 */
    public ServerPlayer getPlayer() {
        return player;
    }

    /** 变化前的数量。 */
    public int getPreviousAmount() {
        return previousAmount;
    }

    /** 变化后的数量，可能为负数。 */
    public int getAmount() {
        return amount;
    }

    /** 变化量，等于 {@link #getAmount()} 减 {@link #getPreviousAmount()}。 */
    public int getDelta() {
        return amount - previousAmount;
    }

    /** 变化原因。 */
    public Cause getCause() {
        return cause;
    }

    /**
     * 玩家回响珍珠数量变化后派发，不可取消。数量没有变化时不会派发。
     */
    public static final class Changed extends EnderEchoPearlEvent {
        public Changed(ServerPlayer player, int previousAmount, int amount, Cause cause) {
            super(player, previousAmount, amount, cause);
        }
    }
}
