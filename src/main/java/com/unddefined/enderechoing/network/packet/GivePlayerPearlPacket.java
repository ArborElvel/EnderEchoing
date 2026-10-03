package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.api.pearl.EnderEchoPearls;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.unddefined.enderechoing.api.event.EnderEchoPearlEvent.Cause.*;
import static com.unddefined.enderechoing.server.registry.DataRegistry.POSITION;

public record GivePlayerPearlPacket(ItemStack itemStack) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "give_player_item");
    public static final Type<GivePlayerPearlPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, GivePlayerPearlPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> ItemStack.STREAM_CODEC.encode(buf, packet.itemStack),
            buf -> new GivePlayerPearlPacket(ItemStack.STREAM_CODEC.decode(buf))
    );

    public static void handle(GivePlayerPearlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 在服务端给玩家物品
            if (context.player() instanceof ServerPlayer S) {
                S.getInventory().add(packet.itemStack);
                if (packet.itemStack.get(POSITION) == null)
                    EnderEchoPearls.set(S, Math.max(EnderEchoPearls.get(S) - packet.itemStack.getCount(), 0), CONVERT);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
