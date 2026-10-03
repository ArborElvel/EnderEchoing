package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.api.event.EnderEchoClientEvent;
import com.unddefined.enderechoing.client.renderer.EchoRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record SendWaypointNamesPacket(Map<BlockPos, String> waypointNames) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "send_waypoint_names");
    public static final Type<SendWaypointNamesPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SendWaypointNamesPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, ByteBufCodecs.STRING_UTF8, 256),
            SendWaypointNamesPacket::waypointNames, SendWaypointNamesPacket::new);

    @OnlyIn(Dist.CLIENT)
    public static void handle(SendWaypointNamesPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EchoRenderer.WaypointNames = msg.waypointNames();
            NeoForge.EVENT_BUS.post(new EnderEchoClientEvent.WaypointNamesSync(msg.waypointNames()));
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}
}
