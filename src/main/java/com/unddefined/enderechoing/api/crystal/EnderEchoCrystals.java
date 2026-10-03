package com.unddefined.enderechoing.api.crystal;

import com.unddefined.enderechoing.server.DataComponents.EnderEchoCrystalSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 末影回响水晶的对外入口，模组内部也走这里。
 *
 * <p>水晶数据按维度保存，读写都要给出对应的服务端维度；真正改变数据时派发
 * {@link com.unddefined.enderechoing.api.event.EnderEchoCrystalEvent} 下的事件。
 */
public final class EnderEchoCrystals {
    private EnderEchoCrystals() {
    }

    /** 水晶条目快照，只读。 */
    public record Entry(GlobalPos pos, String name) {
    }

    /** 该维度已登记的水晶快照。 */
    public static List<Entry> all(ServerLevel level) {
        return EnderEchoCrystalSavedData.get(level).getAll().stream()
                .map(entry -> new Entry(entry.pos(), entry.name()))
                .toList();
    }

    /**
     * 派发可取消的 {@link com.unddefined.enderechoing.api.event.EnderEchoCrystalEvent.Pre}。
     *
     * <p>通过后再放置方块与实体，最后调用 {@link #add}。
     */
    public static boolean canAdd(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        return EnderEchoCrystalSavedData.canAdd(player, dimension, pos);
    }

    /** 登记水晶并派发 Added。 */
    public static boolean add(ServerLevel level, BlockPos pos) {
        return EnderEchoCrystalSavedData.get(level).add(level.dimension(), pos);
    }

    /** 移除水晶并派发 Removed。 */
    public static boolean remove(ServerLevel level, GlobalPos pos) {
        return EnderEchoCrystalSavedData.get(level).remove(pos);
    }

    /** 改写水晶名称并派发 Renamed。 */
    public static boolean rename(ServerLevel level, GlobalPos pos, String name) {
        return EnderEchoCrystalSavedData.get(level).rename(pos, name);
    }
}
