package com.unddefined.enderechoing.api.waypoint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 一条路径点：坐标、名称、图标与是否绑定在末影回响锚点上。
 *
 * <p>这是不可变值类型，玩家列表里的实际内容由 {@link EnderEchoWaypoints} 读写。
 */
public record EnderEchoWaypoint(ResourceKey<Level> dimension, BlockPos pos, String name,
                                int iconIndex, boolean anchorBound) {
    public static final Codec<EnderEchoWaypoint> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(EnderEchoWaypoint::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(EnderEchoWaypoint::pos),
            Codec.STRING.fieldOf("name").forGetter(EnderEchoWaypoint::name),
            Codec.INT.fieldOf("icon").forGetter(EnderEchoWaypoint::iconIndex),
            Codec.BOOL.fieldOf("anchor_bound").forGetter(EnderEchoWaypoint::anchorBound)
    ).apply(builder, EnderEchoWaypoint::new));

    public static final StreamCodec<FriendlyByteBuf, EnderEchoWaypoint> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION),
            EnderEchoWaypoint::dimension,
            BlockPos.STREAM_CODEC,
            EnderEchoWaypoint::pos,
            ByteBufCodecs.STRING_UTF8,
            EnderEchoWaypoint::name,
            ByteBufCodecs.VAR_INT,
            EnderEchoWaypoint::iconIndex,
            ByteBufCodecs.BOOL,
            EnderEchoWaypoint::anchorBound,
            EnderEchoWaypoint::new
    );
}
