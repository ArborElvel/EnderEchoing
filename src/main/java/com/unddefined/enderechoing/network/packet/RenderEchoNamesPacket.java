package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.api.event.EnderEchoClientEvent;
import com.unddefined.enderechoing.client.renderer.ResonatorNameRenderer;
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

public record RenderEchoNamesPacket(Map<BlockPos, String> waypointNames) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "render_echo_names");
    public static final Type<RenderEchoNamesPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RenderEchoNamesPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, ByteBufCodecs.STRING_UTF8, 256),
            RenderEchoNamesPacket::waypointNames, RenderEchoNamesPacket::new);

    @OnlyIn(Dist.CLIENT)
    public static void handle(RenderEchoNamesPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ResonatorNameRenderer.posName = msg.waypointNames();
            NeoForge.EVENT_BUS.post(new EnderEchoClientEvent.ResonatorNamesSync(msg.waypointNames()));
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {return TYPE;}
}
