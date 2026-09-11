package com.unddefined.enderechoing.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.particles.EchoResponding;
import com.unddefined.enderechoing.client.particles.EchoResponse;
import com.unddefined.enderechoing.client.particles.EchoSounding;
import com.unddefined.enderechoing.network.packet.TeleportRequestPacket;
import com.unddefined.enderechoing.server.ShadowNight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.biome.Biomes;
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
import static com.unddefined.enderechoing.server.registry.MobEffectRegistry.SCULK_INTRUSION;
import static com.unddefined.enderechoing.server.registry.MobEffectRegistry.SCULK_VEIL;

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
    private static int sculkveilCountTicks = -43;
    private static int deepDarkCountTicks = -43;
    private static int teleportTicks = 0;
    private static int responseTime = 120;
    private static boolean isCounting = false;
    private static boolean isTeleporting = false;
    private static long lastTickGameTime = -1;

    @SubscribeEvent
    public static void renderEcho(RenderLevelStageEvent event) {
        if (mc.player == null) return;
        float PartialTicks = event.getPartialTick().getGameTimeDeltaTicks();
        boolean hasEffect = mc.player.hasEffect(SCULK_VEIL);
        boolean inDeepDark = mc.level.getBiome(mc.player.blockPosition()).is(Biomes.DEEP_DARK)
                || ShadowNight.isActive(mc.level);
        boolean hasIntrusion = mc.player.hasEffect(SCULK_INTRUSION);
        // 深暗之域掩码重建：渲染线程执行（避免跨线程读 mc.level），
        // 每 100 tick 或移动 64 格重建一次。
//        int px = mc.player.getBlockX(), py = mc.player.getBlockY(), pz = mc.player.getBlockZ();
//        if (SculkVeilRenderer.DEEP_DARK.maskCenterX == Integer.MIN_VALUE
//                || Math.abs(px - SculkVeilRenderer.DEEP_DARK.maskCenterX) > 64
//                || Math.abs(pz - SculkVeilRenderer.DEEP_DARK.maskCenterZ) > 64
//                || mc.player.tickCount % 100 == 0) {
//            SculkVeilRenderer.DEEP_DARK.updateDeepDarkMask(px, py, pz);
//        }
        // buff 渲染器：影匿效果驱动；深暗之域内让位给 DEEP_DARK，避免重复渲染。
        SculkVeilRenderer.BUFF.updateFadeProgress(hasEffect && !inDeepDark, PartialTicks);
        SculkVeilRenderer.DEEP_DARK.DARKNESS_STRENGTH = hasEffect ? 1f : 0f;
        SculkVeilRenderer.DEEP_DARK.fogDensity = hasEffect ? 0.15f : 0.06f;
        SculkVeilRenderer.DEEP_DARK.updateFadeProgress(inDeepDark, PartialTicks);
        SculkIntrusionRenderer.INSTANCE.updateFadeProgress(hasIntrusion, PartialTicks);
        if (!isCounting && SculkVeilRenderer.BUFF.fadeProgress == 0f
                && SculkVeilRenderer.DEEP_DARK.fadeProgress == 0f
                && SculkIntrusionRenderer.INSTANCE.fadeProgress == 0f) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        // AFTER_LEVEL 在 LevelRenderer.renderLevel 返回后才触发：此时世界渲染已完成，
        // 主 framebuffer 里是最终画面（Iris 的 composite + final pass 也已写入其中），
        // 之后脚本还有 drawInHand / GUI 等，因此这里是"不被光影影响且能叠加到画面上"的唯一安全时机。
        // world-render 矩阵在 AFTER_LEVEL 时不再存在于 RenderSystem 中（renderLevel 已把栈 pop 掉，
        // Iris 模式下更是被换成了全屏四边形/合成用的正交矩阵）。存下来以便在渲染前显式恢复。
        renderWorldEffects(event.getPoseStack(), PartialTicks, event.getModelViewMatrix(), event.getProjectionMatrix());
    }

    private static void renderWorldEffects(PoseStack poseStack, float partialTicks, Matrix4f modelView, Matrix4f projection) {
        int tick = countdownTicks < 59 ? countdownTicks : countTicks;
        var bufferSource = mc.renderBuffers().bufferSource();
        if (modelView != null && projection != null && SculkVeilRenderer.BUFF.fadeProgress != 0f)
            SculkVeilRenderer.BUFF.render(sculkveilCountTicks, partialTicks, modelView, projection);
        if (modelView != null && projection != null && SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f)
            SculkVeilRenderer.DEEP_DARK.render(deepDarkCountTicks, partialTicks, modelView, projection);

        // 立即模式四边形（EchoSounding/EchoResponse/EchoResponding）在 endBatch() 刷入 GPU 时，
        // 使用的始终是 RenderSystem.getModelViewMatrix()/getProjectionMatrix()（见 BufferUploader），
        // 而不是我们缓存的矩阵。因此必须显式把世界渲染矩阵恢复到 RenderSystem，否则渲染空间会错。
        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        if (modelView != null) modelViewStack.mul(modelView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        if (projection != null) RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

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
                echoMap.forEach((p, e) -> {
                    boolean hovering = e.render(mc.player, poseStack, bufferSource,
                            countTicks - 40 - responseTime, countdownTicks < 59,
                            MarkedPositionNames.getOrDefault(p, null));
                    if (hovering && !mc.player.isCurrentlyGlowing())
                        EchoResponding.render(poseStack, bufferSource, p, teleportTicks);
                });
            }
            bufferSource.endBatch();
            // 幽匿侵扰最后叠加：覆盖影匿雾与回响特效，GUI 仍绘制在其上层。
            if (SculkIntrusionRenderer.INSTANCE.fadeProgress != 0f)
                SculkIntrusionRenderer.INSTANCE.render(partialTicks);
        } finally {
            RenderSystem.enableDepthTest();
            // 恢复 RenderSystem 状态，避免影响后续手部/GUI 渲染。
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
        // 客户端会给模拟范围内的每个玩家实体触发 tick（本地 + 附近的其他玩家）。
        // 渲染器状态属于本地玩家：按客户端游戏 tick 去重，保证每个 tick 只推进一次，
        // 并始终用本地玩家状态计算；远端玩家的 tick 既不能加速也不能重置本地效果。
        if (mc.player == null || mc.level == null) return;
        long gameTime = mc.level.getGameTime();
        if (gameTime == lastTickGameTime) return;
        lastTickGameTime = gameTime;
        // 速度调整：每个游戏 tick 推进两次（等效 2x）。
        // 发包/状态切换有 isTeleporting 等守卫，不会在同一 tick 内重复发送。
        tickEffects();
        tickEffects();
    }

    private static void tickEffects() {
        var player = mc.player;
        var level = mc.level;
        if (EchoSoundingPos != null && EchoSoundingPos.equals(BlockPos.ZERO)) reset();
        if (targetPos != null && targetPos.pos().equals(BlockPos.ZERO)) targetPos = null;
        if (SculkVeilRenderer.BUFF.fadeProgress != 0f) sculkveilCountTicks++;
        else sculkveilCountTicks = -43;
        if (SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f) deepDarkCountTicks++;
        else deepDarkCountTicks = -43;
        if (teleportTicks > 82 && !player.isCurrentlyGlowing() && !isTeleporting) {
            PacketDistributor.sendToServer(new TeleportRequestPacket(targetPos,false));
            echoMap.remove(targetPos.pos());
            isTeleporting = true;
        }
        if (targetPos != null && targetPreseted && !isTeleporting) {
            if (level.dimension().equals(targetPos.dimension()))
                echoMap.putIfAbsent(targetPos.pos(), new EchoResponse(targetPos.pos()));
            teleportTicks++;
            SculkVeilRenderer.BUFF.fogRadius = Math.max(1f, SculkVeilRenderer.BUFF.fogRadius - 0.18f);
        }
        if (EchoSoundingPos != null) {
            isCounting = true;
            countdownTicks = 60;
            if (player.hasEffect(SCULK_VEIL)) responseTime = 120;
            else responseTime = 30;
            if (echoMap.isEmpty() && !syncedTeleporterPositions.isEmpty()) {
                for (BlockPos pos : syncedTeleporterPositions) {
                    if (pos.equals(EchoSoundingPos)) continue;
                    if (!new AABB(EchoSoundingPos).inflate(EchoSoundingDistance.get()).contains(Vec3.atCenterOf(pos))) continue;
                    echoMap.putIfAbsent(pos, new EchoResponse(pos));
                }
            }
        }
        // The callback may add/remove entries (see the teleport branch below),
        // so iterate over a snapshot instead of modifying HashMap while it is
        // being traversed.
        new HashMap<>(echoMap).forEach((p, e) -> {
            if (e.isElementHovering) {
                teleportTicks++;
                SculkVeilRenderer.BUFF.fogRadius = Math.max(1f, SculkVeilRenderer.BUFF.fogRadius - 0.15f);
                e.hoveringTicks++;
                targetPos = new GlobalPos(level.dimension(), p);
                if (teleportTicks > 40 && !player.isCurrentlyGlowing() && !isTeleporting) {
                    isTeleporting = true;
                    echoMap.putIfAbsent(EchoSoundingPos, new EchoResponse(EchoSoundingPos));
                    echoMap.remove(targetPos.pos());
                    PacketDistributor.sendToServer(new TeleportRequestPacket(targetPos,false));
                }
            }
            if (!targetPreseted && countTicks > responseTime && targetPos != null && targetPos.pos().equals(p) && !e.isElementHovering) {
                teleportTicks = 0;
                SculkVeilRenderer.BUFF.fogRadius = 12f;
            }
        });
        countTicks = isCounting ? countTicks + 1 : 0;
        if (countdownTicks == 0) {
            isCounting = false;
            echoMap.clear();
            reset();
            return;
        }
        countdownTicks--;
        if (EchoSoundingPos == null) return;
        // 玩家离开了方块，重置状态
        if (!new AABB(EchoSoundingPos).intersects(player.getBoundingBox())) reset();
    }

    private static void reset() {
        EchoSoundingPos = null;
        syncedTeleporterPositions.clear();
        targetPreseted = false;
        targetPos = null;
        teleportTicks = 0;
        SculkVeilRenderer.BUFF.fogRadius = 12f;
//        sculkveilCountTicks = -43;
        isTeleporting = false;
    }
}
