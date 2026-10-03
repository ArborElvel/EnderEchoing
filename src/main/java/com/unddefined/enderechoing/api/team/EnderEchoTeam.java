package com.unddefined.enderechoing.api.team;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 队伍的只读快照。
 *
 * <p>{@link EnderEchoTeams#teamOf} 返回本类型；改动队伍请调用 {@link EnderEchoTeams}
 * 的操作入口，事件见 {@link com.unddefined.enderechoing.api.event.EnderEchoTeamEvent}。
 */
public record EnderEchoTeam(UUID teamId, @Nullable UUID captain, List<UUID> members,
                            Map<UUID, UUID> captainVotes) {
    public EnderEchoTeam {
        members = List.copyOf(members);
        captainVotes = Map.copyOf(captainVotes);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}
