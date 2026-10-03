package com.unddefined.enderechoing.network.packet;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.api.team.EnderEchoTeams;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RemoveTeamMemberPacket(UUID targetId) implements CustomPacketPayload {
    public static final Type<RemoveTeamMemberPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            EnderEchoing.MODID, "remove_team_member"));
    public static final StreamCodec<FriendlyByteBuf, RemoveTeamMemberPacket> STREAM_CODEC = StreamCodec.ofMember(
            (packet, buf) -> buf.writeUUID(packet.targetId()),
            buf -> new RemoveTeamMemberPacket(buf.readUUID())
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer caller)) return;
            EnderEchoTeams.RemoveResult result = EnderEchoTeams.removeMember(caller, targetId);
            switch (result) {
                case REMOVED -> {
                    caller.sendSystemMessage(Component.translatable(
                            "message.enderechoing.team.remove_success", resolveName(caller, targetId)));
                    ServerPlayer target = caller.server.getPlayerList().getPlayer(targetId);
                    if (target != null) target.sendSystemMessage(Component.translatable(
                            "message.enderechoing.team.remove_kicked"));
                }
                case LEFT -> caller.sendSystemMessage(Component.translatable("message.enderechoing.team.leave_success"));
                case NOT_IN_TEAM -> caller.sendSystemMessage(Component.translatable("message.enderechoing.team.not_in_team"));
                case TARGET_NOT_IN_TEAM -> caller.sendSystemMessage(Component.translatable(
                        "message.enderechoing.team.captain_target_not_in_team", resolveName(caller, targetId)));
                case NO_PERMISSION -> caller.sendSystemMessage(Component.translatable(
                        "message.enderechoing.team.remove_no_permission"));
                case CANCELLED -> caller.sendSystemMessage(Component.translatable(
                        "message.enderechoing.team.remove_cancelled"));
            }
        });
    }

    private static String resolveName(ServerPlayer caller, UUID playerId) {
        ServerPlayer online = caller.server.getPlayerList().getPlayer(playerId);
        if (online != null) return online.getGameProfile().getName();
        return caller.server.getProfileCache()
                .get(playerId).map(profile -> profile.getName()).orElse("?");
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
