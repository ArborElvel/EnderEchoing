package com.unddefined.enderechoing.api.team;

import com.unddefined.enderechoing.api.event.EnderEchoTeamEvent;
import com.unddefined.enderechoing.api.pearl.EnderEchoPearls;
import com.unddefined.enderechoing.server.team.PlayerTeam;
import com.unddefined.enderechoing.server.team.PlayerTeamSavedData;
import com.unddefined.enderechoing.api.anchor.EnderEchoAnchors;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoint;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoints;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.unddefined.enderechoing.api.event.EnderEchoPearlEvent.Cause.*;

/**
 * 末影回响队伍的对外入口，模组内部也走这里。
 *
 * <p>队伍数据本身存在 {@code PlayerTeamSavedData} 里；本类只提供操作入口，并在
 * 真正发生变更时派发 {@link EnderEchoTeamEvent} 下的各类事件。
 */
public final class EnderEchoTeams {
    public static final int TEAM_TAB_INDEX = 9;

    private EnderEchoTeams() {
    }

    public enum InviteResult {
        SUCCESS_CREATED,
        SUCCESS_JOINED,
        DENIED_SELF,
        DENIED_ALREADY_IN_TEAM,
        DENIED_NO_PERMISSION,
        DENIED_CANCELLED;

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
        NO_PERMISSION,
        CANCELLED
    }

    public static PlayerTeamSavedData data(MinecraftServer server) {
        return PlayerTeamSavedData.get(server);
    }

    /** 玩家所在队伍的只读快照，没有队伍时返回 {@code null}。 */
    @Nullable
    public static EnderEchoTeam teamOf(MinecraftServer server, UUID playerId) {
        PlayerTeam team = PlayerTeamSavedData.get(server).teamOf(playerId);
        if (team == null) return null;
        return new EnderEchoTeam(team.teamId(), team.captain(), team.members(), team.captainVotes());
    }

    public static InviteResult invite(MinecraftServer server, UUID inviterId, UUID targetId) {
        if (inviterId.equals(targetId)) return InviteResult.DENIED_SELF;
        PlayerTeamSavedData data = PlayerTeamSavedData.get(server);
        if (data.teamOf(targetId) != null) return InviteResult.DENIED_ALREADY_IN_TEAM;

        PlayerTeam team = data.teamOf(inviterId);
        if (team != null) {
            if (team.captain() != null && !team.captain().equals(inviterId))
                return InviteResult.DENIED_NO_PERMISSION;

            if (!firePreJoin(team.teamId(), inviterId, targetId, false)) return InviteResult.DENIED_CANCELLED;
            team.addMember(targetId);
            data.setDirty();
            NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.Joined(team.teamId(), targetId, false, team.members()));
            return InviteResult.SUCCESS_JOINED;
        }

        if (!firePreJoin(null, inviterId, targetId, true)) return InviteResult.DENIED_CANCELLED;
        PlayerTeam created = new PlayerTeam(UUID.randomUUID(), List.of(inviterId, targetId), null);
        data.addTeam(created);
        NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.Joined(created.teamId(), targetId, true, created.members()));
        return InviteResult.SUCCESS_CREATED;
    }

    public static boolean removeMember(MinecraftServer server, UUID playerId) {
        PlayerTeamSavedData data = PlayerTeamSavedData.get(server);
        PlayerTeam team = data.teamOf(playerId);
        if (team == null) return false;
        if (!firePreLeave(team.teamId(), playerId, false)) return false;
        team.removeMember(playerId);
        data.setDirty();
        if (team.isEmpty()) data.removeTeam(team.teamId());
        NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.Left(team.teamId(), playerId, false));
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

        if (!firePreLeave(team.teamId(), targetId, leaving)) return RemoveResult.CANCELLED;
        team.removeMember(targetId);
        data.setDirty();
        if (team.isEmpty()) data.removeTeam(team.teamId());
        NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.Left(team.teamId(), targetId, leaving));
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
        NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.VoteCast(team.teamId(), caller.getUUID(), targetId));

        if (team.captainVotes().size() == team.members().size()) {
            Set<UUID> candidates = team.captainVotes().values().stream().collect(Collectors.toSet());
            if (candidates.size() == 1) {
                UUID elected = candidates.iterator().next();
                if (!elected.equals(captain) && !firePreCaptainChanged(team.teamId(), captain, elected))
                    return CaptainVoteResult.VOTE_RECORDED;
                team.setCaptain(elected);
                data.setDirty();
                if (!elected.equals(captain))
                    NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.CaptainChanged(team.teamId(), captain, elected));
                return elected.equals(captain) ? CaptainVoteResult.ALREADY_CAPTAIN : CaptainVoteResult.CAPTAIN_ELECTED;
            }
        }
        return CaptainVoteResult.VOTE_RECORDED;
    }

    /**
     * Copies one of the sharer's marked positions into every other online team member's
     * private list under the team tab, recomputing the anchor-bound state and name
     * wrapper against each recipient's own activated resonator list.
     *
     * @return true when at least one recipient received the copy.
     */
    public static boolean shareToTeam(ServerPlayer sharer, EnderEchoWaypoint M) {
        PlayerTeam team = PlayerTeamSavedData.get(sharer.server).teamOf(sharer.getUUID());
        if (team == null) return false;

        boolean shared = false;
        for (UUID memberId : team.members()) {
            if (memberId.equals(sharer.getUUID())) continue;
            var member = sharer.server.getPlayerList().getPlayer(memberId);
            if (member == null) continue;

            // 路径点 Pre 被拦截时这一位接收方直接跳过：不派发分享事件、不收费
            if (!EnderEchoWaypoints.add(member, M.dimension(), M.pos(), M.name(), M.iconIndex(), M.anchorBound()))
                continue;
            NeoForge.EVENT_BUS.post(new EnderEchoTeamEvent.WaypointShared(team.teamId(), sharer.getUUID(),
                    memberId, M.dimension(), M.pos(), M.name()));
            // 绑定在锚点上的路径点：接收方也补登记，否则这个点只有分享者能用
            if (M.anchorBound()) {
                var level = sharer.server.getLevel(M.dimension());
                if (level != null && EnderEchoAnchors.isAnchor(level, M.pos())
                        && EnderEchoAnchors.canRegister(member, level, M.pos()))
                    EnderEchoAnchors.add(member, level, M.pos());
            }
            EnderEchoPearls.add(member, -1, SHARE);
            shared = true;
        }
        return shared;
    }

    private static boolean firePreJoin(UUID teamId, UUID inviterId, UUID targetId, boolean creatingTeam) {
        var event = new EnderEchoTeamEvent.PreJoin(teamId, inviterId, targetId, creatingTeam);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    private static boolean firePreLeave(UUID teamId, UUID playerId, boolean voluntary) {
        var event = new EnderEchoTeamEvent.PreLeave(teamId, playerId, voluntary);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    private static boolean firePreCaptainChanged(UUID teamId, UUID previousCaptain, UUID newCaptain) {
        var event = new EnderEchoTeamEvent.PreCaptainChanged(teamId, previousCaptain, newCaptain);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
