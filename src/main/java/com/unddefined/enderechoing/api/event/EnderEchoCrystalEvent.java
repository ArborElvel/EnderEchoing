package com.unddefined.enderechoing.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 末影回响水晶事件。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类 {@link Pre}、{@link Added} 与 {@link Removed}。
 * 事件派发在 {@code NeoForge.EVENT_BUS} 上。
 *
 * <p>水晶数据按维度保存，不属于任何玩家，所以事件里没有 owner。
 * 水晶名称是之后单独写入的，改名不会派发事件。
 */
public abstract class EnderEchoCrystalEvent extends Event {
    private final GlobalPos pos;

    protected EnderEchoCrystalEvent(GlobalPos pos) {
        this.pos = pos;
    }

    /** 水晶坐标。 */
    public GlobalPos getPos() {
        return pos;
    }

    /** 水晶所在维度。 */
    public ResourceKey<Level> getDimension() {
        return pos.dimension();
    }

    /** 水晶所在方块位置。 */
    public BlockPos getBlockPos() {
        return pos.pos();
    }

    /**
     * 水晶登记进维度数据后派发，不可取消。
     */
    public static final class Added extends EnderEchoCrystalEvent {
        public Added(GlobalPos pos) {
            super(pos);
        }
    }

    /**
     * 水晶即将放置并登记前派发，可取消。
     *
     * <p>取消后不会放置方块、不会生成水晶实体、不会消耗物品。
     */
    public static final class Pre extends EnderEchoCrystalEvent implements ICancellableEvent {
        private final ServerPlayer player;

        public Pre(ServerPlayer player, GlobalPos pos) {
            super(pos);
            this.player = player;
        }

        /** 尝试放置水晶的玩家。 */
        public ServerPlayer getPlayer() {
            return player;
        }
    }

    /**
     * 水晶从维度数据移除后派发，不可取消。
     */
    public static final class Removed extends EnderEchoCrystalEvent {
        public Removed(GlobalPos pos) {
            super(pos);
        }
    }

    /**
     * 水晶名称被改写后派发，不可取消。写入的名称与之前相同时不会派发。
     */
    public static final class Renamed extends EnderEchoCrystalEvent {
        private final String name;

        public Renamed(GlobalPos pos, String name) {
            super(pos);
            this.name = name;
        }

        /** 水晶的新名称。 */
        public String getName() {
            return name;
        }
    }
}
