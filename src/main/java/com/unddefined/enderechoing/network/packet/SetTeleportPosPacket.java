package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.api.event.EnderEchoClientEvent;
import com.unddefined.enderechoing.client.renderer.EchoRenderer;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetTeleportPosPacket(GlobalPos targetPos, boolean targetPreseted) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "set_teleport_pos");
    public static final Type<SetTeleportPosPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SetTeleportPosPacket> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) -> {buf.writeGlobalPos(msg.targetPos);buf.writeBoolean(msg.targetPreseted);},
             buf -> new SetTeleportPosPacket(buf.readGlobalPos(), buf.readBoolean())
    );

    @OnlyIn(Dist.CLIENT)
    public static void handle(SetTeleportPosPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EchoRenderer.targetPos = msg.targetPos;
            EchoRenderer.targetPreseted = msg.targetPreseted;
            NeoForge.EVENT_BUS.post(new EnderEchoClientEvent.TargetChanged(msg.targetPos, msg.targetPreseted));
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}
}
