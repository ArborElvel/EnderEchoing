package com.unddefined.enderechoing.server.team;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.api.team.EnderEchoTeams;
import com.unddefined.enderechoing.api.team.EnderEchoTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

@EventBusSubscriber(modid = EnderEchoing.MODID)
public class TeamCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("enderecho_team")
                .then(Commands.literal("captain")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return castCaptainVote(context.getSource(), target);
                                }))));
    }

    private static int castCaptainVote(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer caller = source.getPlayerOrException();
        EnderEchoTeams.CaptainVoteResult result = EnderEchoTeams.castCaptainVote(caller, target.getUUID());
        String targetName = target.getGameProfile().getName();
        switch (result) {
            case CAPTAIN_ELECTED -> {
                EnderEchoTeam team = EnderEchoTeams.teamOf(caller.server, caller.getUUID());
                if (team != null) {
                    for (UUID memberId : team.members()) {
                        ServerPlayer member = caller.server.getPlayerList().getPlayer(memberId);
                        if (member != null) member.sendSystemMessage(Component.translatable(
                                "message.enderechoing.team.captain_elected", targetName));
                    }
                }
            }
            case VOTE_RECORDED -> {
                EnderEchoTeam team = EnderEchoTeams.teamOf(caller.server, caller.getUUID());
                caller.sendSystemMessage(Component.translatable("message.enderechoing.team.captain_vote_recorded",
                        targetName,
                        team != null ? team.captainVotes().size() : 0,
                        team != null ? team.members().size() : 0));
            }
            case NOT_IN_TEAM -> caller.sendSystemMessage(Component.translatable("message.enderechoing.team.not_in_team"));
            case TARGET_NOT_IN_TEAM -> caller.sendSystemMessage(Component.translatable(
                    "message.enderechoing.team.captain_target_not_in_team", targetName));
            case ALREADY_CAPTAIN -> caller.sendSystemMessage(Component.translatable(
                    "message.enderechoing.team.already_captain", targetName));
        }
        return 1;
    }
}
