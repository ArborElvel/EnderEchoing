package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.api.pearl.EnderEchoPearls;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoint;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoints;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.unddefined.enderechoing.api.event.EnderEchoPearlEvent.Cause.*;
import static com.unddefined.enderechoing.server.registry.DataRegistry.ICON_LIST;
import static net.minecraft.network.codec.ByteBufCodecs.INT;

public record SyncTunerDataPacket(List<ItemStack> iconList,
                                  List<EnderEchoWaypoint> waypointsCache,
                                  int ee_pearl_amount) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTunerDataPacket> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncTunerDataPacket::iconList,
            EnderEchoWaypoint.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncTunerDataPacket::waypointsCache,
            INT.cast(),
            SyncTunerDataPacket::ee_pearl_amount,
            SyncTunerDataPacket::new
    );

    public static final Type<SyncTunerDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "sync_icon_list"));

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player().setData(ICON_LIST.get(), iconList);
            EnderEchoPearls.set(context.player(), ee_pearl_amount, SYNC);
            EnderEchoWaypoints.replace(context.player(), waypointsCache);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}
}
