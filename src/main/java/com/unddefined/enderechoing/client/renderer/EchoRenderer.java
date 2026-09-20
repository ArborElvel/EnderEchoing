package com.unddefined.enderechoing.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.particles.EchoResponding;
import com.unddefined.enderechoing.client.particles.EchoResponse;
import com.unddefined.enderechoing.client.particles.EchoSounding;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.network.packet.TeleportRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unddefined.enderechoing.Config.EchoSoundingDistance;

@EventBusSubscriber(modid = EnderEchoing.MODID, value = Dist.CLIENT)
public class EchoRenderer {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<BlockPos, EchoResponse> echoMap = new HashMap<>();
    public static BlockPos EchoSoundingPos = null;
    public static boolean targetPreseted = false;
    public static GlobalPos targetPos = null;
    public static List<BlockPos> syncedTeleporterPositions = new ArrayList<>();
    public static Map<BlockPos, String> MarkedPositionNames = new HashMap<>();
    private static int countTicks = 0;
    private static int countdownTicks = 60;
    private static int teleportTicks = 0;
    private static int responseTime = 30;
    private static boolean isCounting = false;
    private static boolean isTeleporting = false;
    private static boolean renderedBySculkVeilDriver = false;
    private static long lastTickGameTime = -1;

    /**
     * 供 sculkborne 的影匿后处理在幽匿雾画完之后回调：两个 mod 同时加载时由它在 AFTER_LEVEL 最后叠加回响波，
     * 避免雾把波盖住。同一帧内 {@link #renderEcho} 不会再画第二遍。
     */
    public static void renderEchoAfterSculkVeil(RenderLevelStageEvent event) {
        if (!canRender(event)) return;
        renderWorldEffects(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaTicks(),
                event.getModelViewMatrix(), event.getProjectionMatrix());
        renderedBySculkVeilDriver = true;
    }

    @SubscribeEvent
    public static void renderEcho(RenderLevelStageEvent event) {
        if (!canRender(event)) return;
        if (renderedBySculkVeilDriver) {
            renderedBySculkVeilDriver = false;
            return;
        }
        renderWorldEffects(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaTicks(),
                event.getModelViewMatrix(), event.getProjectionMatrix());
    }

    private static boolean canRender(RenderLevelStageEvent event) {
        return mc.player != null && isCounting && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL;
    }

    private static void renderWorldEffects(PoseStack poseStack, float partialTicks, Matrix4f modelView, Matrix4f projection) {
        int tick = countdownTicks < 59 ? countdownTicks : countTicks;
        var bufferSource = mc.renderBuffers().bufferSource();
        if (modelView == null || projection == null) return;

        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(modelView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

        try {
            mc.getMainRenderTarget().bindWrite(false);
            RenderSystem.disableDepthTest();
            if (targetPreseted) {
                EchoSounding.render(poseStack, bufferSource, partialTicks, tick - 20, LightTexture.FULL_BRIGHT);
                if (targetPos != null && echoMap.containsKey(targetPos.pos())) {
                    echoMap.get(targetPos.pos()).render(mc.player, poseStack, bufferSource,
                            teleportTicks - 80, false, null);
                    if (teleportTicks > 60)
                        EchoResponding.render(poseStack, bufferSource, targetPos.pos(), teleportTicks);
                }
            }
            if (tick > 20)
                EchoSounding.render(poseStack, bufferSource, partialTicks, tick - 20, LightTexture.FULL_BRIGHT);

            if (!targetPreseted && countTicks > responseTime && !echoMap.isEmpty()) {
                echoMap.forEach((pos, response) -> {
                    boolean hovering = response.render(mc.player, poseStack, bufferSource,
                            countTicks - 40 - responseTime, countdownTicks < 59,
                            MarkedPositionNames.getOrDefault(pos, null));
                    if (hovering && !mc.player.isCurrentlyGlowing())
                        EchoResponding.render(poseStack, bufferSource, pos, teleportTicks);
                });
            }
            bufferSource.endBatch();
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.restoreProjectionMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        long gameTime = mc.level.getGameTime();
        if (gameTime == lastTickGameTime) return;
        lastTickGameTime = gameTime;
        tickEffects();
        tickEffects();
    }

    private static void tickEffects() {
        var player = mc.player;
        var level = mc.level;
        if (EchoSoundingPos != null && EchoSoundingPos.equals(BlockPos.ZERO)) reset();
        if (targetPos != null && targetPos.pos().equals(BlockPos.ZERO)) targetPos = null;
        if (teleportTicks > 82 && targetPos != null && !player.isCurrentlyGlowing() && !isTeleporting) {
            PacketDistributor.sendToServer(new TeleportRequestPacket(targetPos, false));
            echoMap.remove(targetPos.pos());
            isTeleporting = true;
        }
        if (targetPos != null && targetPreseted && !isTeleporting) {
            if (level.dimension().equals(targetPos.dimension()))
                echoMap.putIfAbsent(targetPos.pos(), new EchoResponse(targetPos.pos()));
            teleportTicks++;
        }
        if (EchoSoundingPos != null) {
            isCounting = true;
            countdownTicks = 60;
            responseTime = SculkBorneBridge.hasVeil(player) ? 120 : 30;
            if (echoMap.isEmpty() && !syncedTeleporterPositions.isEmpty()) {
                for (BlockPos pos : syncedTeleporterPositions) {
                    if (pos.equals(EchoSoundingPos)) continue;
                    if (!new AABB(EchoSoundingPos).inflate(EchoSoundingDistance.get()).contains(Vec3.atCenterOf(pos))) continue;
                    echoMap.putIfAbsent(pos, new EchoResponse(pos));
                }
            }
        }
        new HashMap<>(echoMap).forEach((pos, response) -> {
            if (response.isElementHovering) {
                teleportTicks++;
                response.hoveringTicks++;
                targetPos = new GlobalPos(level.dimension(), pos);
                if (teleportTicks > 40 && !player.isCurrentlyGlowing() && !isTeleporting) {
                    isTeleporting = true;
                    echoMap.putIfAbsent(EchoSoundingPos, new EchoResponse(EchoSoundingPos));
                    echoMap.remove(targetPos.pos());
                    PacketDistributor.sendToServer(new TeleportRequestPacket(targetPos, false));
                }
            }
            if (!targetPreseted && countTicks > responseTime && targetPos != null && targetPos.pos().equals(pos) && !response.isElementHovering) {
                teleportTicks = 0;
            }
        });
        countTicks = isCounting ? countTicks + 1 : 0;
        if (countdownTicks == 0) {
            reset();
            return;
        }
        countdownTicks--;
        if (EchoSoundingPos == null) return;
        if (!new AABB(EchoSoundingPos).intersects(player.getBoundingBox())) reset();
    }

    private static void reset() {
        EchoSoundingPos = null;
        syncedTeleporterPositions.clear();
        targetPreseted = false;
        targetPos = null;
        teleportTicks = 0;
        isTeleporting = false;
        renderedBySculkVeilDriver = false;
    }
}
