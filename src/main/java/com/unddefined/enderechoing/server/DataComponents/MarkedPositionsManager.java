package com.unddefined.enderechoing.server.DataComponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import com.unddefined.enderechoing.server.registry.DataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public record MarkedPositionsManager(List<MarkedPositionsManager.Teleporters> teleporters,
                                     List<MarkedPositionsManager.MarkedPositions> markedPositions) implements INBTSerializable<Tag> {

    public MarkedPositionsManager() {
        this(new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>());
    }

    public MarkedPositionsManager(List<MarkedPositionsManager.Teleporters> teleporters, List<MarkedPositionsManager.MarkedPositions> markedPositions) {
        this.teleporters = new CopyOnWriteArrayList<>(teleporters);
        this.markedPositions = new CopyOnWriteArrayList<>(markedPositions);
    }

    public static MarkedPositionsManager getManager(Player player) {
        return player.getData(DataRegistry.MARKED_POSITIONS_CACHE.get());
    }

    public void addTeleporter(Level level, BlockPos pos) {
        var T = new MarkedPositionsManager.Teleporters(new GlobalPos(level.dimension(), pos));
        teleporters.stream().filter(e-> e.equals(T)).findFirst()
                .ifPresentOrElse(e-> {}, () -> teleporters.add(T));
        checkBounds();
    }

    public boolean isTeleporter(Level level, BlockPos pos) {
        var B = level.getBlockEntity(pos);
        return B instanceof EnderEchoicResonatorBlockEntity || B instanceof WarpPlatformBlockEntity;
    }

    public boolean checkTeleporter(Level level, BlockPos pos) {
        AtomicBoolean flag = new AtomicBoolean(false);
        teleporters.stream().filter(e -> e.dimension().equals(level.dimension()))
                .filter(e -> e.pos().equals(pos)).findFirst()
                .ifPresentOrElse(e -> {}, () -> flag.set(true));
        return isTeleporter(level, pos) && flag.get();
    }

    public boolean addMarkedPosition(ResourceKey<Level> dimension, BlockPos pos, String name, int iconIndex, boolean teleporterBound) {
        if (pos == null || name == null || dimension == null) return false;
        markedPositions.removeIf(entry -> entry.dimension.equals(dimension) && entry.pos.equals(pos));
        markedPositions.add(new MarkedPositionsManager.MarkedPositions(dimension, pos, name, iconIndex, teleporterBound));
        return true;
    }

    public GlobalPos getNearestTeleporter(Level level, BlockPos fromPos) {
        GlobalPos nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MarkedPositionsManager.Teleporters entry : teleporters) {
            // 只检查同一维度的传送器
            if (entry.dimension().equals(level.dimension())) {
                double distance = entry.pos().distSqr(fromPos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPos = entry.globalPos;
                }
            }
        }
        return nearestPos;
    }

    public List<BlockPos> getTeleporterPositions(Level level) {
        return teleporters.stream()
                .filter(e -> e.dimension().equals(level.dimension()))
                .map(Teleporters::pos).collect(Collectors.toList());
    }

    public Map<BlockPos, String> getMarkedTeleportersMap(Level level) {
        checkBounds();
        Map<BlockPos, String> resultMap = new HashMap<>();
        markedPositions.stream().filter(entry -> entry.dimension.equals(level.dimension()))
                .filter(entry -> entry.teleporterBound)
                .forEach(entry -> resultMap.put(entry.pos, entry.name));
        return resultMap;
    }

    public void checkBounds() {
        // 1. 将 teleporters 转为 Set，将查找的时间复杂度从 O(N) 降到 O(1)
        Set<String> teleporterKeys = teleporters.stream()
                .map(T -> T.dimension() + "_" + T.pos())
                .collect(Collectors.toSet());

        // 2. 使用 replaceAll 原地替换列表中的元素
        markedPositions.replaceAll(e -> {
            boolean isMatch = teleporterKeys.contains(e.dimension() + "_" + e.pos());
            // 构造一个新的 record 实例来替换旧实例
            return new MarkedPositionsManager.MarkedPositions(
                    e.dimension(), e.pos(),
                    isMatch ? (e.name().startsWith(">") && e.name().endsWith("<") ? e.name() : ">" + e.name + "<")
                            : e.name().replaceAll("^[><]+|[><]+$", ""),
                    e.iconIndex(), isMatch
            );
        });
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        // Serialize teleporters list
        ListTag teleportersTag = new ListTag();
        for (MarkedPositionsManager.Teleporters teleporter : teleporters) {
            teleportersTag.add(GlobalPos.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), teleporter.globalPos())
                    .getOrThrow(IllegalStateException::new));
        }
        tag.put("teleporters", teleportersTag);

        // Serialize marked positions list
        ListTag markedPositionsTag = new ListTag();
        for (MarkedPositionsManager.MarkedPositions markedPosition : markedPositions) {
            markedPositionsTag.add(MarkedPositionsManager.MarkedPositions.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), markedPosition)
                    .getOrThrow(IllegalStateException::new));
        }
        tag.put("marked_positions", markedPositionsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (!(nbt instanceof CompoundTag tag)) return;

        // Deserialize teleporters list
        teleporters.clear();
        if (tag.contains("teleporters")) {
            ListTag teleportersTag = tag.getList("teleporters", 10); // 10 is compound tag type
            for (Tag value : teleportersTag)
                GlobalPos.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), value)
                        .resultOrPartial(error -> {
                        }).ifPresent(globalPos -> teleporters.add(new MarkedPositionsManager.Teleporters(globalPos)));
        }

        // Deserialize marked positions list
        markedPositions.clear();
        if (tag.contains("marked_positions")) {
            ListTag markedPositionsTag = tag.getList("marked_positions", 10); // 10 is compound tag type
            for (Tag value : markedPositionsTag)
                MarkedPositionsManager.MarkedPositions.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), value)
                        .resultOrPartial(error -> {
                        }).ifPresent(markedPositions::add);
        }
    }

    public record Teleporters(GlobalPos globalPos) {
        public ResourceKey<Level> dimension() {
            return globalPos.dimension();
        }

        public BlockPos pos() {
            return globalPos.pos();
        }
    }

    public record MarkedPositions(ResourceKey<Level> dimension, BlockPos pos, String name, int iconIndex, boolean teleporterBound) {
        public static final Codec<MarkedPositionsManager.MarkedPositions> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(MarkedPositionsManager.MarkedPositions::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(MarkedPositionsManager.MarkedPositions::pos),
                Codec.STRING.fieldOf("name").forGetter(MarkedPositionsManager.MarkedPositions::name),
                Codec.INT.fieldOf("icon").forGetter(MarkedPositionsManager.MarkedPositions::iconIndex),
                Codec.BOOL.fieldOf("teleporter_bound").forGetter(MarkedPositions::teleporterBound)
        ).apply(builder, MarkedPositionsManager.MarkedPositions::new));

        public static final StreamCodec<FriendlyByteBuf, MarkedPositions> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION),
                MarkedPositions::dimension,
                BlockPos.STREAM_CODEC,
                MarkedPositions::pos,
                ByteBufCodecs.STRING_UTF8,
                MarkedPositions::name,
                ByteBufCodecs.VAR_INT,
                MarkedPositions::iconIndex,
                ByteBufCodecs.BOOL,
                MarkedPositions::teleporterBound,
                MarkedPositions::new
        );
    }
}