package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.Config;
import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.blocks.entity.EnderEchoTunerBlockEntity;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.util.Utils;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_AMOUNT;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.WARP_CORE;

public record TeleportRequestPacket(GlobalPos targetPos, boolean canWarp) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "teleporter_request");
    public static final Type<TeleportRequestPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, TeleportRequestPacket> STREAM_CODEC = StreamCodec.ofMember(
            (msg, buf) ->{buf.writeGlobalPos(msg.targetPos);buf.writeBoolean(msg.canWarp);},
             buf -> new TeleportRequestPacket(buf.readGlobalPos(),buf.readBoolean())
    );

    public static void handle(TeleportRequestPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (msg.targetPos == null) return;
            var pos = msg.targetPos.pos();
            var level = player.level();
            var blockEntity = level.getBlockEntity(player.blockPosition().above(2));
            boolean crossDimension = !level.dimension().equals(msg.targetPos.dimension());
            boolean needsPearl = !MarkedPositionsManager.getManager(player).getMarkedTeleportersMap(level).containsKey(pos);
            var playerList = Utils.getNearEchoPlayers(level, player);
            // 传送前记下出发地，传送成功后起点与终点都可能刷出幽匿螨
            var fromPos = player.position();
            if (!crossDimension) {
                player.teleportTo(pos.getCenter().x, pos.getCenter().y, pos.getCenter().z);
                playerList.forEach( p -> p.teleportTo(pos.getCenter().x, pos.getCenter().y, pos.getCenter().z));
            } else {
                ServerLevel destination = player.getServer().getLevel(msg.targetPos.dimension());
                if (destination == null) return;
                player.changeDimension(new DimensionTransition(destination, pos.getCenter(), player.getDeltaMovement(),
                        player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
                playerList.forEach( p -> p.changeDimension(new DimensionTransition(destination, pos.getCenter(), player.getDeltaMovement(),
                        player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND)));
            }

            // 传送成功：让 sculkborne 决定要不要在起点与终点刷出幽匿螨
            if (player instanceof ServerPlayer serverPlayer) SculkBorneBridge.afterTeleport(serverPlayer, level, fromPos);

            // 只有传送成功后才扣除费用。
            int cost = (needsPearl && !msg.canWarp ? 1 : 0) + (crossDimension ? 1 : 0);
            if (cost > 0) player.setData(EE_PEARL_AMOUNT, player.getData(EE_PEARL_AMOUNT) - cost);
            if (msg.canWarp) {
                player.getCooldowns().addCooldown(WARP_CORE.asItem(), Config.ENDER_ECHOING_CORE_COOLDOWN.get() * 5);
                return;
            }
            if (crossDimension && blockEntity instanceof EnderEchoTunerBlockEntity tuner) tuner.consumeAnchorCharge();
            if (blockEntity instanceof EnderEchoTunerBlockEntity tuner) tuner.setSelectedPosition(GZERO, "");
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
