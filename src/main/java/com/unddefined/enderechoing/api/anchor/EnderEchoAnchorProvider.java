package com.unddefined.enderechoing.api.anchor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 末影回响锚点判定器：回答“这个位置能不能当作末影回响锚点”。
 *
 * <p>这里判定的锚点就是玩家数据里的 {@code anchors}，与路径点
 * （{@code waypoints}）不是一回事。判定结果会参与珍珠绑定、调谐器登记、
 * 登录时失效点清理与队伍分享等流程，因此实现必须是纯查询，不能有副作用。
 * 调用发生在服务端逻辑路径上，但请勿假定 {@code level} 一定是服务端实例。
 *
 * <p>实现只能依赖 {@code level} 与 {@code pos} 本身，不得假定加载顺序，
 * 也不要在实现里访问尚未注册的内容。
 */
@FunctionalInterface
public interface EnderEchoAnchorProvider {
    /**
     * @param level 目标位置所在维度
     * @param pos   待判定的方块位置
     * @return 该位置是否为末影回响锚点
     */
    boolean isAnchor(Level level, BlockPos pos);
}
