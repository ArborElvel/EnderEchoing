package com.unddefined.enderechoing.client.api;

import com.unddefined.enderechoing.client.renderer.EchoRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 客户端侧同步数据的只读入口。
 *
 * <p>这里的值由服务端发包写入，其他模组可以据此画自己的标记、小地图或 HUD，
 * 不需要自己解析末影回响的网络包。本类只在物理客户端可用。
 */
public final class EnderEchoClientData {
    private EnderEchoClientData() {
    }

    /** 已同步到客户端的锚点坐标（服务端在回响谐振时下发）。 */
    public static List<BlockPos> anchors() {
        return List.copyOf(EchoRenderer.syncedAnchorPositions);
    }

    /** 已同步的路径点名称：坐标 → 名称。也可能是死亡标记等带 ☠ 前缀的名字。 */
    public static Map<BlockPos, String> waypointNames() {
        return Map.copyOf(EchoRenderer.WaypointNames);
    }

    /** 客户端当前预览的传送目的地，没有选中时为 {@code null}。 */
    @Nullable
    public static GlobalPos echoTarget() {
        return EchoRenderer.targetPos;
    }

    /** 客户端是否处于“已选好目的地、等待确认”的状态。 */
    public static boolean targetPreset() {
        return EchoRenderer.targetPreseted;
    }
}
