package com.unddefined.enderechoing.api.event;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 末影回响仪器事件：调谐器与折跃平台的目的地变更。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link PositionChanged}。事件派发在 {@code NeoForge.EVENT_BUS} 上。
 */
public abstract class EnderEchoDeviceEvent extends Event {
    private final GlobalPos device;
    private final GlobalPos target;
    private final String name;

    protected EnderEchoDeviceEvent(GlobalPos device, GlobalPos target, String name) {
        this.device = device;
        this.target = target;
        this.name = name;
    }

    /** 仪器自身所在位置。 */
    public GlobalPos getDevicePos() {
        return device;
    }

    /** 仪器当前指向的目的地；目的地被清空时等于 {@code EnderEchoing.GZERO}。 */
    public GlobalPos getTarget() {
        return target;
    }

    /** 目的地名称；目的地被清空时是空字符串。 */
    public String getName() {
        return name;
    }

    /**
     * 仪器目的地变更后派发，不可取消。目的地被清空也会派发。
     */
    public static final class PositionChanged extends EnderEchoDeviceEvent {
        public PositionChanged(GlobalPos device, GlobalPos target, String name) {
            super(device, target, name);
        }
    }

    /**
     * 玩家即将通过调谐界面改写仪器目的地前派发，可取消。
     *
     * <p>只在玩家主动切换目的地时派发；传送结束后由模组内部把目的地复位成
     * {@code EnderEchoing.GZERO} 属于内部状态清理，不经过本事件。
     */
    public static final class Pre extends EnderEchoDeviceEvent implements ICancellableEvent {
        private final ServerPlayer player;

        public Pre(ServerPlayer player, GlobalPos device, GlobalPos target, String name) {
            super(device, target, name);
            this.player = player;
        }

        /** 发起改写的玩家。 */
        public ServerPlayer getPlayer() {
            return player;
        }
    }
}
