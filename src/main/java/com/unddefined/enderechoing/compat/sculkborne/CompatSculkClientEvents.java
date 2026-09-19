package com.unddefined.enderechoing.compat.sculkborne;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.renderer.SculkVeilRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = EnderEchoing.MODID, value = Dist.CLIENT)
public final class CompatSculkClientEvents {
    private static final Minecraft mc = Minecraft.getInstance();
    private static int veilTicks = -43;
    private static int deepDarkTicks = -43;
    private static long lastTickGameTime = -1;

    @SubscribeEvent
    public static void renderSculkVeil(RenderLevelStageEvent event) {
        if (!CompatSculkRegistry.ACTIVE || mc.player == null) return;
        float partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
        boolean hasVeil = SculkBorneBridge.hasVeil(mc.player);
        boolean inDeepDark = mc.level.getBiome(mc.player.blockPosition()).is(Biomes.DEEP_DARK)
                || isShadowNight();

        SculkVeilRenderer.BUFF.updateFadeProgress(hasVeil && !inDeepDark, partialTicks);
        SculkVeilRenderer.DEEP_DARK.DARKNESS_STRENGTH = hasVeil ? 1f : 0f;
        SculkVeilRenderer.DEEP_DARK.fogDensity = hasVeil ? 0.15f : 0.06f;
        SculkVeilRenderer.DEEP_DARK.updateFadeProgress(inDeepDark, partialTicks);
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (SculkVeilRenderer.BUFF.fadeProgress == 0f && SculkVeilRenderer.DEEP_DARK.fadeProgress == 0f) return;

        Matrix4f modelView = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
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
            if (SculkVeilRenderer.BUFF.fadeProgress != 0f)
                SculkVeilRenderer.BUFF.render(veilTicks, partialTicks, modelView, projection);
            if (SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f)
                SculkVeilRenderer.DEEP_DARK.render(deepDarkTicks, partialTicks, modelView, projection);
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.restoreProjectionMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!CompatSculkRegistry.ACTIVE || mc.level == null || mc.player == null) return;
        long gameTime = mc.level.getGameTime();
        if (gameTime == lastTickGameTime) return;
        lastTickGameTime = gameTime;
        if (SculkVeilRenderer.BUFF.fadeProgress != 0f) veilTicks++;
        else veilTicks = -43;
        if (SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f) deepDarkTicks++;
        else deepDarkTicks = -43;
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        veilTicks = -43;
        deepDarkTicks = -43;
        SculkVeilRenderer.BUFF.fogRadius = 12f;
    }

    private static boolean isShadowNight() {
        if (mc.level == null || mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        long time = Math.floorMod(mc.level.dayTime(), 24000L);
        return mc.level.getMoonPhase() == 4 && time >= 13000L && time < 23000L;
    }

    private CompatSculkClientEvents() {}
}
