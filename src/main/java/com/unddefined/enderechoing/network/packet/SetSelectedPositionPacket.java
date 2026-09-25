package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.blocks.entity.EnderEchoTunerBlockEntity;
import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetSelectedPositionPacket(BlockPos blockPos, GlobalPos selectedPos, String name) implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, SetSelectedPositionPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetSelectedPositionPacket::blockPos,
            GlobalPos.STREAM_CODEC,
            SetSelectedPositionPacket::selectedPos,
            ByteBufCodecs.STRING_UTF8,
            SetSelectedPositionPacket::name,
            SetSelectedPositionPacket::new
    );

    public static final Type<SetSelectedPositionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "set_selected_position"));

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            BlockEntity be = player.level().getBlockEntity(blockPos);
            if (be instanceof EnderEchoTunerBlockEntity tuner) tuner.setSelectedPosition(selectedPos, name);
            if (be instanceof WarpPlatformBlockEntity warp) warp.setSelectedPosition(selectedPos, name);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}
}