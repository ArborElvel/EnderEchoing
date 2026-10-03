package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.renderer.EchoRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SendSyncedAnchorPositionsPacket(List<BlockPos> list) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "synced_anchor_positions");
    public static final Type<SendSyncedAnchorPositionsPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SendSyncedAnchorPositionsPacket> STREAM_CODEC =StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(256)),
            SendSyncedAnchorPositionsPacket::list,
            SendSyncedAnchorPositionsPacket::new
    );

    @OnlyIn(Dist.CLIENT)
    public static void handle(SendSyncedAnchorPositionsPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> EchoRenderer.syncedAnchorPositions = msg.list);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}
}
