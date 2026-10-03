package com.unddefined.enderechoing.server.DataComponents;

import com.unddefined.enderechoing.api.event.EnderEchoCrystalEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

public class EnderEchoCrystalSavedData extends SavedData {
    public static final String ID = "ender_echo_crystals";
    public final Set<CrystalEntry> crystals = new HashSet<>();

    public EnderEchoCrystalSavedData() {
    }

    public static EnderEchoCrystalSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(EnderEchoCrystalSavedData::new, EnderEchoCrystalSavedData::load), ID);
    }

    public static EnderEchoCrystalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EnderEchoCrystalSavedData data = new EnderEchoCrystalSavedData();
        ListTag list = tag.getList(ID, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag crystal = (CompoundTag) t;
            var dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(crystal.getString("dimension")));
            GlobalPos pos = GlobalPos.of(dimension, BlockPos.of(crystal.getLong("pos")));
            String name = crystal.getString("name");
            data.crystals.add(new CrystalEntry(pos, name));
        }
        return data;
    }

    // ===== API =====
    /**
     * 派发可取消的 {@link EnderEchoCrystalEvent.Pre}。
     *
     * <p>只负责判定，不修改任何数据；返回 true 的调用方应当继续放置方块与实体，然后调用
     * {@link #add}。非服务端玩家一律放行。
     */
    public static boolean canAdd(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        var event = new EnderEchoCrystalEvent.Pre(serverPlayer, GlobalPos.of(dimension, pos));
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public boolean add(ResourceKey<Level> d, BlockPos p) {
        var globalPos = GlobalPos.of(d, p);
        boolean added = crystals.add(new CrystalEntry(globalPos, ""));
        setDirty();
        if (added) NeoForge.EVENT_BUS.post(new EnderEchoCrystalEvent.Added(globalPos));
        return added;
    }

    public boolean remove(GlobalPos pos) {
        boolean removed = crystals.removeIf(g -> g.pos.equals(pos));
        setDirty();
        if (removed) NeoForge.EVENT_BUS.post(new EnderEchoCrystalEvent.Removed(pos));
        return removed;
    }

    /**
     * 改写已登记水晶的名称。
     *
     * @return true 表示找到了该水晶并写入；名称没有变化时也返回 true，但不会派发事件
     */
    public boolean rename(GlobalPos pos, String name) {
        for (CrystalEntry entry : crystals) {
            if (!entry.pos.equals(pos)) continue;
            boolean changed = !entry.name.equals(name);
            entry.setName(name);
            setDirty();
            if (changed) NeoForge.EVENT_BUS.post(new EnderEchoCrystalEvent.Renamed(pos, name));
            return true;
        }
        return false;
    }

    public Set<CrystalEntry> getAll() {
        return Collections.unmodifiableSet(crystals);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (CrystalEntry entry : crystals) {
            CompoundTag crystal = new CompoundTag();
            crystal.putString("dimension", entry.pos().dimension().location().toString());
            crystal.putLong("pos", entry.pos().pos().asLong());
            crystal.putString("name", entry.name());
            list.add(crystal);
        }
        tag.put(ID, list);
        return tag;
    }

    public static class CrystalEntry {
        private final GlobalPos pos;
        private String name = "";

        public CrystalEntry(GlobalPos pos, String name) {
            this.pos = pos;
            this.name = name;
        }

        public GlobalPos pos() {
            return pos;
        }

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
