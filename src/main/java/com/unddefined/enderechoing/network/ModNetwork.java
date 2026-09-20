package com.unddefined.enderechoing.network;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.network.packet.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Supplier;

@EventBusSubscriber(modid = EnderEchoing.MODID)
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1.0";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // 注册物品重命名数据包
        registrar.playToServer(
                PearlRenamePacket.TYPE,
                PearlRenamePacket.STREAM_CODEC,
                PearlRenamePacket::handle
        );

        // 注册打开编辑屏幕数据包
        registerClientPayload(registrar,
                OpenEditScreenPacket.TYPE,
                OpenEditScreenPacket.STREAM_CODEC,
                () -> OpenEditScreenPacket::handle
        );

        // 注册添加效果数据包
        registrar.playToServer(
                AddEffectPacket.TYPE,
                AddEffectPacket.STREAM_CODEC,
                AddEffectPacket::handle
        );

        // 注册传送数据包
        registrar.playToServer(
                TeleportRequestPacket.TYPE,
                TeleportRequestPacket.STREAM_CODEC,
                TeleportRequestPacket::handle
        );

        registrar.playToServer(
                SetSelectedPositionPacket.TYPE,
                SetSelectedPositionPacket.STREAM_CODEC,
                SetSelectedPositionPacket::handle
        );

        registrar.playToServer(
                GivePlayerPearlPacket.TYPE,
                GivePlayerPearlPacket.STREAM_CODEC,
                GivePlayerPearlPacket::handle
        );

        // 注册同步Tuner数据包
        registrar.playToServer(
                SyncTunerDataPacket.TYPE,
                SyncTunerDataPacket.STREAM_CODEC,
                SyncTunerDataPacket::handle
        );

        registrar.playToServer(
                SetTunerSelectedTabPacket.TYPE,
                SetTunerSelectedTabPacket.STREAM_CODEC,
                SetTunerSelectedTabPacket::handle
        );
        registerClientPayload(registrar,
                SetTeleportPosPacket.TYPE,
                SetTeleportPosPacket.STREAM_CODEC,
                () -> SetTeleportPosPacket::handle
        );
        registerClientPayload(registrar,
                SetEchoSoundingPosPacket.TYPE,
                SetEchoSoundingPosPacket.STREAM_CODEC,
                () -> SetEchoSoundingPosPacket::handle
        );
        registerClientPayload(registrar,
                SendMarkedPositionNamesPacket.TYPE,
                SendMarkedPositionNamesPacket.STREAM_CODEC,
                () -> SendMarkedPositionNamesPacket::handle
        );

        registerClientPayload(registrar,
                RenderEchoNamesPacket.TYPE,
                RenderEchoNamesPacket.STREAM_CODEC,
                () -> RenderEchoNamesPacket::handle
        );

        registerClientPayload(registrar,
                SendSyncedTeleporterPositionsPacket.TYPE,
                SendSyncedTeleporterPositionsPacket.STREAM_CODEC,
                () -> SendSyncedTeleporterPositionsPacket::handle
        );

        // 注册结构信息请求和回复数据包
        registrar.playToServer(
                RequestStructureInfoPacket.TYPE,
                RequestStructureInfoPacket.STREAM_CODEC,
                RequestStructureInfoPacket::handle
        );
        registerClientPayload(registrar,
                ReplyStructureInfoPacket.TYPE,
                ReplyStructureInfoPacket.STREAM_CODEC,
                () -> ReplyStructureInfoPacket::handle
        );

        // 注册维度列表请求和回复数据包
        registrar.playToServer(
                RequestDimensionListPacket.TYPE,
                RequestDimensionListPacket.STREAM_CODEC,
                RequestDimensionListPacket::handle
        );
        registerClientPayload(registrar,
                ReplyDimensionListPacket.TYPE,
                ReplyDimensionListPacket.STREAM_CODEC,
                () -> ReplyDimensionListPacket::handle
        );

        // 注册设置充能状态数据包
        registrar.playToServer(
                SetUnchargedPacket.TYPE,
                SetUnchargedPacket.STREAM_CODEC,
                SetUnchargedPacket::handle
        );

        registrar.playToServer(
                RequestPlayerDataPacket.TYPE,
                RequestPlayerDataPacket.STREAM_CODEC,
                RequestPlayerDataPacket::handle
        );
        registerClientPayload(registrar,
                ReplyPlayerDataPacket.TYPE,
                ReplyPlayerDataPacket.STREAM_CODEC,
                () -> ReplyPlayerDataPacket::handle
        );

        registrar.playToServer(
                RemoveTeamMemberPacket.TYPE,
                RemoveTeamMemberPacket.STREAM_CODEC,
                RemoveTeamMemberPacket::handle
        );

        registrar.playToServer(
                ShareToTeamPacket.TYPE,
                ShareToTeamPacket.STREAM_CODEC,
                ShareToTeamPacket::handle
        );
    }

    /**
     * Client-bound payloads must keep their real handler only on the client. A dedicated
     * server still registers the payload type so channel negotiation matches, but with a
     * no-op handler, since the client-only handle is never executed there.
     * The handler is supplied lazily so the stripped {@code @OnlyIn(CLIENT)} handle is
     * never resolved on a dedicated server.
     */
    private static <T extends CustomPacketPayload> void registerClientPayload(
            PayloadRegistrar registrar, CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<IPayloadHandler<T>> clientHandler) {

        if (FMLEnvironment.dist.isClient()) registrar.playToClient(type, codec, clientHandler.get());
        else registrar.playToClient(type, codec, (payload, context) -> {});
    }
}
