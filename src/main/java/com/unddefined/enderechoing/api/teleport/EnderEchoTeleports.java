package com.unddefined.enderechoing.api.teleport;

import com.unddefined.enderechoing.api.event.EnderEchoTeleportEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 传送前后统一入口：各传送装置都调用这里，模组内部也一样。
 *
 * <p>传送前调用 {@link #canTeleport} 派发可取消的 {@link EnderEchoTeleportEvent.Pre}；
 * 传送成功后调用 {@link #afterTeleport} 派发 {@link EnderEchoTeleportEvent.Post}。
 * 新增传送装置时必须成对调用，并提供传送前维度、传送前坐标与目标坐标，顺序不能颠倒。
 */
public final class EnderEchoTeleports {
    private EnderEchoTeleports() {
    }

    /**
     * 传送执行前派发可取消事件。
     *
     * @return true 表示没有被取消，调用方应当继续传送；false 表示被依赖方阻止，调用方必须直接返回
     */
    public static boolean canTeleport(ServerPlayer player, Level fromLevel, Vec3 fromPos, GlobalPos target) {
        var event = new EnderEchoTeleportEvent.Pre(player, fromLevel, fromPos, target);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public static void afterTeleport(ServerPlayer player, Level fromLevel, Vec3 fromPos, GlobalPos target) {
        NeoForge.EVENT_BUS.post(new EnderEchoTeleportEvent.Post(player, fromLevel, fromPos, target));
    }
}
