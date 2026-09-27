package com.unddefined.enderechoing.server.team;

import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_AMOUNT;

public final class TeamManager {
    public static final int TEAM_TAB_INDEX = 9;

    private TeamManager() {
    }

    public enum InviteResult {
        SUCCESS_CREATED,
        SUCCESS_JOINED,
        DENIED_SELF,
        DENIED_ALREADY_IN_TEAM,
        DENIED_NO_PERMISSION;

        public boolean success() {
            return this == SUCCESS_CREATED || this == SUCCESS_JOINED;
        }
    }

    public enum CaptainVoteResult {
        NOT_IN_TEAM,
        TARGET_NOT_IN_TEAM,
        ALREADY_CAPTAIN,
        VOTE_RECORDED,
        CAPTAIN_ELECTED
    }

    public enum RemoveResult {
        REMOVED,
        LEFT,
        NOT_IN_TEAM,
        TARGET_NOT_IN_TEAM,
        NO_PERMISSION
    }

    public static PlayerTeamSavedData data(MinecraftServer server) {
        return PlayerTeamSavedData.get(server);
    }

    @Nullable
    public static PlayerTeam teamOf(MinecraftServer server, UUID playerId) {
        return PlayerTeamSavedData.get(server).teamOf(playerId);
    }

    public static InviteResult invite(MinecraftServer server, UUID inviterId, UUID targetId) {
        if (inviterId.equals(targetId)) return InviteResult.DENIED_SELF;
        PlayerTeamSavedData data = PlayerTeamSavedData.get(server);
        if (data.teamOf(targetId) != null) return InviteResult.DENIED_ALREADY_IN_TEAM;

        PlayerTeam team = data.teamOf(inviterId);
        if (team != null) {
            if (team.captain() != null && !team.captain().equals(inviterId))
                return InviteResult.DENIED_NO_PERMISSION;

            team.addMember(targetId);
            data.setDirty();
            return InviteResult.SUCCESS_JOINED;
        }

        data.addTeam(new PlayerTeam(UUID.randomUUID(), List.of(inviterId, targetId), null));
        return InviteResult.SUCCESS_CREATED;
    }

    public static boolean removeMember(MinecraftServer server, UUID playerId) {
        PlayerTeamSavedData data = PlayerTeamSavedData.get(server);
        PlayerTeam team = data.teamOf(playerId);
        if (team == null) return false;
        team.removeMember(playerId);
        data.setDirty();
        if (team.isEmpty()) data.removeTeam(team.teamId());
        return true;
    }

    /**
     * Removes a member on behalf of a caller. Anyone may leave by removing themself;
     * removing other members requires no captain yet, or the caller to be the captain.
     */
    public static RemoveResult removeMember(ServerPlayer caller, UUID targetId) {
        PlayerTeamSavedData data = PlayerTeamSavedData.get(caller.server);
        PlayerTeam team = data.teamOf(caller.getUUID());
        if (team == null) return RemoveResult.NOT_IN_TEAM;
        if (!team.isMember(targetId)) return RemoveResult.TARGET_NOT_IN_TEAM;

        boolean leaving = caller.getUUID().equals(targetId);
        if (!leaving && team.captain() != null && !team.captain().equals(caller.getUUID())) {
            return RemoveResult.NO_PERMISSION;
        }

        team.removeMember(targetId);
        data.setDirty();
        if (team.isEmpty()) data.removeTeam(team.teamId());
        return leaving ? RemoveResult.LEFT : RemoveResult.REMOVED;
    }

    /**
     * Records one member's vote for the given candidate. The captain changes only when
     * every member has voted and all votes point to the same candidate.
     */
    public static CaptainVoteResult castCaptainVote(ServerPlayer caller, UUID targetId) {
        PlayerTeamSavedData data = PlayerTeamSavedData.get(caller.server);
        PlayerTeam team = data.teamOf(caller.getUUID());
        if (team == null) return CaptainVoteResult.NOT_IN_TEAM;
        if (!team.isMember(targetId)) return CaptainVoteResult.TARGET_NOT_IN_TEAM;

        UUID captain = team.captain();
        if (captain != null && captain.equals(targetId) && team.captainVotes().isEmpty()) {
            return CaptainVoteResult.ALREADY_CAPTAIN;
        }

        team.castCaptainVote(caller.getUUID(), targetId);
        data.setDirty();

        if (team.captainVotes().size() == team.members().size()) {
            Set<UUID> candidates = team.captainVotes().values().stream().collect(Collectors.toSet());
            if (candidates.size() == 1) {
                UUID elected = candidates.iterator().next();
                team.setCaptain(elected);
                data.setDirty();
                return elected.equals(captain) ? CaptainVoteResult.ALREADY_CAPTAIN : CaptainVoteResult.CAPTAIN_ELECTED;
            }
        }
        return CaptainVoteResult.VOTE_RECORDED;
    }

    /**
     * Copies one of the sharer's marked positions into every other online team member's
     * private list under the team tab, recomputing the teleporter-bound state and name
     * wrapper against each recipient's own activated resonator list.
     *
     * @return true when at least one recipient received the copy.
     */
    public static boolean shareToTeam(ServerPlayer sharer, MarkedPositionsManager.MarkedPositions M) {
        PlayerTeam team = PlayerTeamSavedData.get(sharer.server).teamOf(sharer.getUUID());
        if (team == null) return false;

        boolean shared = false;
        for (UUID memberId : team.members()) {
            if (memberId.equals(sharer.getUUID())) continue;
            var member = sharer.server.getPlayerList().getPlayer(memberId);
            if (member == null) continue;

            var memberManager = MarkedPositionsManager.getManager(member);
            memberManager.addMarkedPosition(M.dimension(), M.pos(), M.name(), M.iconIndex(), M.teleporterBound());
            // 绑定在传送点上的路径点：接收方也补登记，否则这个点只有分享者能用
            if (M.teleporterBound()) {
                var level = sharer.server.getLevel(M.dimension());
                if (level != null && memberManager.isTeleporter(level, M.pos()))
                    memberManager.addTeleporter(level, M.pos());
            }
            member.setData(EE_PEARL_AMOUNT,member.getData(EE_PEARL_AMOUNT) - 1);
            shared = true;
        }
        return shared;
    }
}
