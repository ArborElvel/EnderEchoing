package com.unddefined.enderechoing.api.pearl;

import com.unddefined.enderechoing.api.event.EnderEchoPearlEvent;
import com.unddefined.enderechoing.server.registry.DataRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 回响珍珠数量的唯一读写入口，模组内部也走这里。
 *
 * <p>所有对 {@code EE_PEARL_AMOUNT} 的修改都要走这里，只有数值真的变化时才派发
 * {@link EnderEchoPearlEvent.Changed}；客户端侧修改不会派发。数量允许为负数，不做钳制。
 */
public final class EnderEchoPearls {
    private EnderEchoPearls() {
    }

    public static int get(Player player) {
        return player.getData(DataRegistry.EE_PEARL_AMOUNT.get());
    }

    /** 在当前数量上增减，允许结果为负数。 */
    public static void add(Player player, int delta, EnderEchoPearlEvent.Cause cause) {
        set(player, get(player) + delta, cause);
    }

    /** 直接设定数量，不做钳制。 */
    public static void set(Player player, int amount, EnderEchoPearlEvent.Cause cause) {
        int previous = get(player);
        if (previous == amount) return;
        player.setData(DataRegistry.EE_PEARL_AMOUNT.get(), amount);
        if (player instanceof ServerPlayer serverPlayer)
            NeoForge.EVENT_BUS.post(new EnderEchoPearlEvent.Changed(serverPlayer, previous, amount, cause));
    }
}
