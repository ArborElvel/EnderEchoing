package com.unddefined.enderechoing.api.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.Event;

/**
 * 末影回响结构相关事件。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link Visited}。事件派发在 {@code NeoForge.EVENT_BUS} 上。
 */
public abstract class EnderEchoStructureEvent extends Event {
    private final ServerPlayer player;
    private final ResourceKey<Structure> structure;
    private final ChunkPos chunk;

    protected EnderEchoStructureEvent(ServerPlayer player, ResourceKey<Structure> structure, ChunkPos chunk) {
        this.player = player;
        this.structure = structure;
        this.chunk = chunk;
    }

    /** 触发记录的玩家。 */
    public ServerPlayer getPlayer() {
        return player;
    }

    /** 结构类型。 */
    public ResourceKey<Structure> getStructure() {
        return structure;
    }

    /** 结构起始区块，与该结构一起唯一确定一次记录。 */
    public ChunkPos getChunk() {
        return chunk;
    }

    /**
     * 玩家进入某个可被回响之眼定位的结构、且该结构此前未记录过时派发，不可取消。
     *
     * <p>判定每秒执行一次，但只有真正写入新的“已访问结构”时才会派发，
     * 因此同一结构在同一区块对同一玩家只派发一次。可扩展的结构范围见
     * {@code enderechoing:ender_echoing_eye_located} 结构标签。
     */
    public static final class Visited extends EnderEchoStructureEvent {
        public Visited(ServerPlayer player, ResourceKey<Structure> structure, ChunkPos chunk) {
            super(player, structure, chunk);
        }
    }
}
