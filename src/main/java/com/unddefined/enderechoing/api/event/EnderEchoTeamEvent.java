package com.unddefined.enderechoing.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 末影回响队伍相关事件。
 *
 * <p>基类是抽象的：NeoForge 事件总线不允许对抽象事件注册监听，
 * 依赖方请监听具体子类。事件派发在 {@code NeoForge.EVENT_BUS} 上。
 *
 * <p>这些事件由 {@code EnderEchoTeams} 的入口派发；绕过该入口直接改动队伍模型
 * 的成员或队长不会派发事件。
 */
public abstract class EnderEchoTeamEvent extends Event {
    private final UUID teamId;

    protected EnderEchoTeamEvent(UUID teamId) {
        this.teamId = teamId;
    }

    /** 队伍 id，玩家离开后该 id 可能已不再对应任何队伍。 */
    public UUID getTeamId() {
        return teamId;
    }

    /**
     * 有玩家加入队伍后派发，不可取消。
     *
     * <p>新建队伍时只派发一次，{@link #getPlayerId()} 是被邀请者，
     * 发起邀请的玩家不会单独派发；用 {@link #getMembers()} 可以拿到包含发起者的完整成员快照。
     */
    public static final class Joined extends EnderEchoTeamEvent {
        private final UUID playerId;
        private final boolean teamCreated;
        private final List<UUID> members;

        public Joined(UUID teamId, UUID playerId, boolean teamCreated, List<UUID> members) {
            super(teamId);
            this.playerId = playerId;
            this.teamCreated = teamCreated;
            this.members = List.copyOf(members);
        }

        /** 加入队伍的玩家。 */
        public UUID getPlayerId() {
            return playerId;
        }

        /** 该玩家加入的是否是刚建立的新队伍。 */
        public boolean isTeamCreated() {
            return teamCreated;
        }

        /** 事件派发时的成员快照，不含之后的变化。 */
        public List<UUID> getMembers() {
            return members;
        }
    }

    /**
     * 有玩家离开队伍或被移出队伍后派发，不可取消。
     */
    public static final class Left extends EnderEchoTeamEvent {
        private final UUID playerId;
        private final boolean voluntary;

        public Left(UUID teamId, UUID playerId, boolean voluntary) {
            super(teamId);
            this.playerId = playerId;
            this.voluntary = voluntary;
        }

        /** 离开或被移出队伍的玩家。 */
        public UUID getPlayerId() {
            return playerId;
        }

        /** true 表示本人主动退出，false 表示被队长移除或由系统移除。 */
        public boolean isVoluntary() {
            return voluntary;
        }
    }

    /**
     * 队长发生变化后派发，不可取消。
     */
    public static final class CaptainChanged extends EnderEchoTeamEvent {
        @Nullable
        private final UUID previousCaptain;
        private final UUID newCaptain;

        public CaptainChanged(UUID teamId, @Nullable UUID previousCaptain, UUID newCaptain) {
            super(teamId);
            this.previousCaptain = previousCaptain;
            this.newCaptain = newCaptain;
        }

        /** 变更前的队长，队伍此前没有队长时为 {@code null}。 */
        @Nullable
        public UUID getPreviousCaptain() {
            return previousCaptain;
        }

        /** 变更后的队长。 */
        public UUID getNewCaptain() {
            return newCaptain;
        }
    }

    /**
     * 有成员投出队长票后派发，不可取消。
     *
     * <p>此时票数不一定已经选出队长；队长真正变化时会再派发 {@link CaptainChanged}。
     */
    public static final class VoteCast extends EnderEchoTeamEvent {
        private final UUID voterId;
        private final UUID candidateId;

        public VoteCast(UUID teamId, UUID voterId, UUID candidateId) {
            super(teamId);
            this.voterId = voterId;
            this.candidateId = candidateId;
        }

        /** 投票的成员。 */
        public UUID getVoterId() {
            return voterId;
        }

        /** 被投票的候选人。 */
        public UUID getCandidateId() {
            return candidateId;
        }
    }

    /**
     * 队伍分享路径点，接收方在自己的列表里写入该点后派发，不可取消。
     *
     * <p>每个接收方各派发一次，分享者本人不会派发。
     */
    public static final class WaypointShared extends EnderEchoTeamEvent {
        private final UUID sharerId;
        private final UUID recipientId;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final String name;

        public WaypointShared(UUID teamId, UUID sharerId, UUID recipientId,
                              ResourceKey<Level> dimension, BlockPos pos, String name) {
            super(teamId);
            this.sharerId = sharerId;
            this.recipientId = recipientId;
            this.dimension = dimension;
            this.pos = pos;
            this.name = name;
        }

        /** 分享者。 */
        public UUID getSharerId() {
            return sharerId;
        }

        /** 接收方。 */
        public UUID getRecipientId() {
            return recipientId;
        }

        /** 被分享路径点所在维度。 */
        public ResourceKey<Level> getDimension() {
            return dimension;
        }

        /** 被分享路径点坐标。 */
        public BlockPos getPos() {
            return pos;
        }

        /** 被分享路径点名称。 */
        public String getName() {
            return name;
        }
    }

    /**
     * 玩家即将被拉入队伍前派发，可取消。
     *
     * <p>取消后不会加入、也不会创建队伍。{@link #getTeamId()} 在“将要新建队伍”时是 {@code null}。
     */
    public static final class PreJoin extends EnderEchoTeamEvent implements ICancellableEvent {
        private final UUID inviterId;
        private final UUID targetId;
        private final boolean creatingTeam;

        public PreJoin(@Nullable UUID teamId, UUID inviterId, UUID targetId, boolean creatingTeam) {
            super(teamId);
            this.inviterId = inviterId;
            this.targetId = targetId;
            this.creatingTeam = creatingTeam;
        }

        /** 发起邀请的玩家。 */
        public UUID getInviterId() {
            return inviterId;
        }

        /** 即将入队的玩家。 */
        public UUID getTargetId() {
            return targetId;
        }

        /** 本次操作是否会新建队伍（此时 {@link #getTeamId()} 为 {@code null}）。 */
        public boolean isCreatingTeam() {
            return creatingTeam;
        }
    }

    /**
     * 玩家即将离开或被移出队伍前派发，可取消。
     */
    public static final class PreLeave extends EnderEchoTeamEvent implements ICancellableEvent {
        private final UUID playerId;
        private final boolean voluntary;

        public PreLeave(UUID teamId, UUID playerId, boolean voluntary) {
            super(teamId);
            this.playerId = playerId;
            this.voluntary = voluntary;
        }

        /** 即将离开或被移出队伍的玩家。 */
        public UUID getPlayerId() {
            return playerId;
        }

        /** true 表示本人主动退出，false 表示被队长移除或由系统移除。 */
        public boolean isVoluntary() {
            return voluntary;
        }
    }

    /**
     * 队长即将变更前派发，可取消。
     *
     * <p>取消后投票已经记录，但队长不变，票选结果按“仅记录”返回；
     * 之后再次达成一致时仍会重新派发本事件。
     */
    public static final class PreCaptainChanged extends EnderEchoTeamEvent implements ICancellableEvent {
        @Nullable
        private final UUID previousCaptain;
        private final UUID newCaptain;

        public PreCaptainChanged(UUID teamId, @Nullable UUID previousCaptain, UUID newCaptain) {
            super(teamId);
            this.previousCaptain = previousCaptain;
            this.newCaptain = newCaptain;
        }

        /** 变更前的队长，队伍此前没有队长时为 {@code null}。 */
        @Nullable
        public UUID getPreviousCaptain() {
            return previousCaptain;
        }

        /** 即将成为队长的玩家。 */
        public UUID getNewCaptain() {
            return newCaptain;
        }
    }
}
