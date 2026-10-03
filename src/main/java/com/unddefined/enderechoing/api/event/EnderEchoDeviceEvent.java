package com.unddefined.enderechoing.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 末影回响仪器事件：调谐器、折跃平台、谐振器与水晶。
 *
 * <p>基类只带仪器自身位置，各子类再带自己的载荷。基类是抽象的：NeoForge 事件总线
 * 不允许对抽象事件注册监听，依赖方请监听具体子类。事件派发在 {@code NeoForge.EVENT_BUS} 上。
 */
public abstract class EnderEchoDeviceEvent extends Event {
    private final GlobalPos device;

    protected EnderEchoDeviceEvent(GlobalPos device) {
        this.device = device;
    }

    /** 仪器自身所在位置。 */
    public GlobalPos getDevicePos() {
        return device;
    }

    /**
     * 仪器每秒一次的服务端判定，不可取消。
     *
     * <p>调谐器、折跃平台、谐振器与水晶方块实体在加载状态下每 20 刻派发一次，
     * 供依赖方按自己的规则决定要不要在仪器附近做什么（例如按概率生成生物）。
     * 判断生成条件时要自己检查难度、加载状态与碰撞空间。
     */
    public static final class Tick extends EnderEchoDeviceEvent {
        private final Level level;
        private final BlockPos pos;

        public Tick(Level level, BlockPos pos) {
            super(GlobalPos.of(level.dimension(), pos));
            this.level = level;
            this.pos = pos;
        }

        /** 仪器所在维度。 */
        public Level getLevel() {
            return level;
        }

        /** 仪器所在方块位置。 */
        public BlockPos getBlockPos() {
            return pos;
        }
    }

    /**
     * 仪器目的地变更后派发，不可取消。目的地被清空也会派发。
     */
    public static final class PositionChanged extends EnderEchoDeviceEvent {
        private final GlobalPos target;
        private final String name;

        public PositionChanged(GlobalPos device, GlobalPos target, String name) {
            super(device);
            this.target = target;
            this.name = name;
        }

        /** 仪器当前指向的目的地；目的地被清空时等于 {@code EnderEchoing.GZERO}。 */
        public GlobalPos getTarget() {
            return target;
        }

        /** 目的地名称；目的地被清空时是空字符串。 */
        public String getName() {
            return name;
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
        private final GlobalPos target;
        private final String name;

        public Pre(ServerPlayer player, GlobalPos device, GlobalPos target, String name) {
            super(device);
            this.player = player;
            this.target = target;
            this.name = name;
        }

        /** 发起改写的玩家。 */
        public ServerPlayer getPlayer() {
            return player;
        }

        /** 即将写入的目的地。 */
        public GlobalPos getTarget() {
            return target;
        }

        /** 即将写入的目的地名称。 */
        public String getName() {
            return name;
        }
    }
}
