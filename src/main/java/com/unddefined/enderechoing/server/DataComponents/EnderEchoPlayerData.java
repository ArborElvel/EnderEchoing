package com.unddefined.enderechoing.server.DataComponents;

import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoint;
import com.unddefined.enderechoing.server.registry.DataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 玩家名下的末影回响数据：锚点与路径点两份列表。
 *
 * <p>只负责存储与 NBT 读写，具体操作的入口在 api 包：{@code EnderEchoAnchors}
 * 与 {@code EnderEchoWaypoints}。
 */
public record EnderEchoPlayerData(List<EnderEchoPlayerData.Anchor> anchors,
                                  List<EnderEchoWaypoint> waypoints) implements INBTSerializable<Tag> {

    public EnderEchoPlayerData() {
        this(new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>());
    }

    public EnderEchoPlayerData(List<EnderEchoPlayerData.Anchor> anchors, List<EnderEchoWaypoint> waypoints) {
        this.anchors = new CopyOnWriteArrayList<>(anchors);
        this.waypoints = new CopyOnWriteArrayList<>(waypoints);
    }

    public static EnderEchoPlayerData getManager(Player player) {
        return player.getData(DataRegistry.WAYPOINT_CACHE.get());
    }

    /**
     * 按当前锚点列表重算路径点的绑定状态与名称标记。
     *
     * <p>锚点列表变动后由 api 侧的读写入口调用。
     */
    public void checkBounds() {
        // 1. 将 anchors 转为 Set，将查找的时间复杂度从 O(N) 降到 O(1)
        Set<String> anchorKeys = anchors.stream()
                .map(T -> T.dimension() + "_" + T.pos())
                .collect(Collectors.toSet());

        // 2. 使用 replaceAll 原地替换列表中的元素
        waypoints.replaceAll(e -> {
            boolean isMatch = anchorKeys.contains(e.dimension() + "_" + e.pos());
            // 构造一个新的 record 实例来替换旧实例
            return new EnderEchoWaypoint(
                    e.dimension(), e.pos(),
                    isMatch ? (e.name().startsWith(">") && e.name().endsWith("<") ? e.name() : ">" + e.name() + "<")
                            : e.name().replaceAll("^[><]+|[><]+$", ""),
                    e.iconIndex(), isMatch
            );
        });
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        // Serialize anchors list
        ListTag anchorsTag = new ListTag();
        for (EnderEchoPlayerData.Anchor anchor : anchors) {
            anchorsTag.add(GlobalPos.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), anchor.globalPos())
                    .getOrThrow(IllegalStateException::new));
        }
        tag.put("anchors", anchorsTag);

        // Serialize waypoints list
        ListTag waypointsTag = new ListTag();
        for (EnderEchoWaypoint waypoint : waypoints) {
            waypointsTag.add(EnderEchoWaypoint.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), waypoint)
                    .getOrThrow(IllegalStateException::new));
        }
        tag.put("waypoints", waypointsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (!(nbt instanceof CompoundTag tag)) return;

        // Deserialize anchors list
        anchors.clear();
        if (tag.contains("anchors")) {
            ListTag anchorsTag = tag.getList("anchors", 10); // 10 is compound tag type
            for (Tag value : anchorsTag)
                GlobalPos.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), value)
                        .resultOrPartial(error -> {
                        }).ifPresent(globalPos -> anchors.add(new EnderEchoPlayerData.Anchor(globalPos)));
        }

        // Deserialize waypoints list
        waypoints.clear();
        if (tag.contains("waypoints")) {
            ListTag waypointsTag = tag.getList("waypoints", 10); // 10 is compound tag type
            for (Tag value : waypointsTag)
                EnderEchoWaypoint.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), value)
                        .resultOrPartial(error -> {
                        }).ifPresent(waypoints::add);
        }
    }

    public record Anchor(GlobalPos globalPos) {
        public ResourceKey<Level> dimension() {
            return globalPos.dimension();
        }

        public BlockPos pos() {
            return globalPos.pos();
        }
    }
}
