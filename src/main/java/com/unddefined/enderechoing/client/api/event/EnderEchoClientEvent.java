package com.unddefined.enderechoing.client.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.Map;

/**
 * 客户端同步事件基类。
 *
 * <p>服务端下发客户端缓存更新后派发，全部不可取消，也只在物理客户端存在。
 * 依赖方请监听具体子类；事件总线不允许监听抽象基类。
 */
public abstract class EnderEchoClientEvent extends Event {
    protected EnderEchoClientEvent() {
    }

    /**
     * 锚点坐标列表同步到客户端后派发。
     */
    public static final class AnchorSync extends EnderEchoClientEvent {
        private final List<BlockPos> anchors;

        public AnchorSync(List<BlockPos> anchors) {
            this.anchors = List.copyOf(anchors);
        }

        /** 本次同步的锚点坐标快照。 */
        public List<BlockPos> getAnchors() {
            return anchors;
        }
    }

    /**
     * 路径点名称同步到客户端后派发（用于回响立体名字渲染）。
     */
    public static final class WaypointNamesSync extends EnderEchoClientEvent {
        private final Map<BlockPos, String> waypointNames;

        public WaypointNamesSync(Map<BlockPos, String> waypointNames) {
            this.waypointNames = Map.copyOf(waypointNames);
        }

        /** 本次同步的「坐标 → 名称」快照。 */
        public Map<BlockPos, String> getWaypointNames() {
            return waypointNames;
        }
    }

    /**
     * 谐振器/水晶名称同步到客户端后派发。
     */
    public static final class ResonatorNamesSync extends EnderEchoClientEvent {
        private final Map<BlockPos, String> resonatorNames;

        public ResonatorNamesSync(Map<BlockPos, String> resonatorNames) {
            this.resonatorNames = Map.copyOf(resonatorNames);
        }

        /** 本次同步的「坐标 → 名称」快照。 */
        public Map<BlockPos, String> getResonatorNames() {
            return resonatorNames;
        }
    }

    /**
     * 客户端预览的传送目的地变化后派发。
     */
    public static final class TargetChanged extends EnderEchoClientEvent {
        private final GlobalPos target;
        private final boolean preset;

        public TargetChanged(GlobalPos target, boolean preset) {
            this.target = target;
            this.preset = preset;
        }

        /** 新的预览目的地。 */
        public GlobalPos getTarget() {
            return target;
        }

        /** 是否处于「已选好目的地、等待确认」的状态。 */
        public boolean isPreset() {
            return preset;
        }
    }
}
