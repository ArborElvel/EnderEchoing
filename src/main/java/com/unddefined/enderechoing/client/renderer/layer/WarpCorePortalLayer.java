package com.unddefined.enderechoing.client.renderer.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.renderer.item.WarpCoreRenderer;
import com.unddefined.enderechoing.client.shader.WarpCorePortalShaders;
import com.unddefined.enderechoing.compat.iris.IrisCompat;
import com.unddefined.enderechoing.items.WarpCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * core 骨的自绘星云层。
 * <p>
 * 不开光影时不用它（走原版 {@link RenderType#endPortal()}）。开光影时，Iris 会跳过它没接管的 shader，
 * 所以只有 GUI 能当场画；手部（第一/第三人称）要按 Re-Avaritia 的做法延迟到
 * {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}——那时光影包的 final pass 已经结束，
 * 主 framebuffer 上的绘制不再被 Iris 拦截。
 */
@EventBusSubscriber(modid = EnderEchoing.MODID, value = Dist.CLIENT)
public final class WarpCorePortalLayer {

    private static final List<DeferredCore> DEFERRED = new ArrayList<>();

    public static final RenderType RENDER_TYPE = RenderType.create(
            EnderEchoing.MODID + ":warp_core_portal",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(WarpCorePortalShaders::shader))
                    // 和原版传送门一样绑两张图：Sampler0 = end_sky.png、Sampler1 = end_portal.png
                    // （portal 开 mipmap，shader 里 textureProjLod 才有模糊层级可拿来做光晕）
                    .setTextureState(RenderStateShard.MultiTextureStateShard.builder()
                            .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                            .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, true)
                            .build())
                    // core 写深度：描边靠它的深度把背面那层挡掉，不能改成只写颜色
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .createCompositeState(false));

    /** 光晕层：同一几何绕 core 中心放大一圈，用加法混合叠出去。 */
    public static final RenderType GLOW_TYPE = RenderType.create(
            EnderEchoing.MODID + ":warp_core_portal_glow",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(WarpCorePortalShaders::shader))
                    .setTextureState(RenderStateShard.MultiTextureStateShard.builder()
                            .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                            .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, true)
                            .build())
                    // 加法混合（src + dst）；GLINT 是 src*src + dst，暗淡的星云会被二次压没
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    // 只写颜色：光晕要靠加法叠在基础层上，不能再写一遍深度
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .createCompositeState(false));

    /** core 立方体中心在骨骼空间里的高度（模型里是 (0, 3, 0) px），几何光晕绕它缩放。 */
    private static final float CORE_CENTER_Y = 3F / 16F;
    /** 几何光晕相对 core 的放大倍数（绕 core 中心缩放）。 */
    private static final float HALO_SCALE = 1.3F;

    private WarpCorePortalLayer() {
    }

    /**
     * 开光影时，只要这次物品渲染发生在 world 渲染阶段就需要延迟（手部是第一/第三人称，平台/展示用的是
     * {@link ItemDisplayContext#NONE} 或 FIXED/GROUND）。GUI 不在 world 渲染阶段，当场画即可；
     * 屏幕里的物品预览（可能也用 NONE/FIXED）同样当场画，否则会画在屏幕之前被界面盖住。
     */
    public static boolean shouldDefer(ItemDisplayContext context) {
        if (context == null || context == ItemDisplayContext.GUI) return false;
        if (!WarpCorePortalShaders.isReady() || !IrisCompat.isShaderPackInUse()) return false;

        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.screen == null;
    }

    /**
     * 记录一次延迟绘制：把 core 骨（含骨骼变换）的绝对 pose、投影/视图矩阵和光照状态存下来，
     * 等 AFTER_LEVEL 再原样重放。
     */
    public static void enqueue(GeoItemRenderer<WarpCore> renderer, GeoBone bone, PoseStack poseStack,
                               Matrix4f projection, Matrix4f modelView,
                               int packedLight, int packedOverlay, int colour) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());
        RenderUtil.prepMatrixForBone(copy, bone);

        DEFERRED.add(new DeferredCore(renderer, bone,
                new Matrix4f(copy.last().pose()), new Matrix3f(copy.last().normal()),
                new Matrix4f(modelView), new Matrix4f(projection),
                packedLight, packedOverlay, colour));
    }

    /** 绕 core 立方体中心把骨骼放大一圈，用加法混合再画一遍，形成芯外辉光。 */
    public static void renderGlow(GeoItemRenderer<WarpCore> renderer, PoseStack poseStack, GeoBone bone,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay, int colour) {
        poseStack.pushPose();
        try {
            poseStack.translate(0F, CORE_CENTER_Y, 0F);
            poseStack.scale(HALO_SCALE, HALO_SCALE, HALO_SCALE);
            poseStack.translate(0F, -CORE_CENTER_Y, 0F);
            renderer.renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(GLOW_TYPE),
                    packedLight, packedOverlay, colour);
        } finally {
            poseStack.popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || DEFERRED.isEmpty()) return;
        renderDeferred(event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }

    private static void renderDeferred(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            DEFERRED.clear();
            return;
        }

        MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f oldModelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        WarpCoreRenderer.applyPortalTint();

        try {
            mc.getMainRenderTarget().bindWrite(false);

            for (DeferredCore call : DEFERRED) {
                RenderSystem.setProjectionMatrix(call.projection(), RenderSystem.getVertexSorting());
                RenderSystem.getModelViewStack().set(call.modelView());
                RenderSystem.applyModelViewMatrix();

                PoseStack poseStack = new PoseStack();
                poseStack.last().pose().set(call.pose());
                poseStack.last().normal().set(call.normal());

                VertexConsumer buffer = source.getBuffer(RENDER_TYPE);
                call.renderer().renderCubesOfBone(poseStack, call.bone(), buffer,
                        call.packedLight(), call.packedOverlay(), call.colour());
                // 基础层先落地，加法光晕才能叠在它上面
                source.endBatch(RENDER_TYPE);

//                renderGlow(call.renderer(), poseStack, call.bone(), source,
//                        call.packedLight(), call.packedOverlay(), call.colour());
//                source.endBatch(GLOW_TYPE);
            }

            source.endBatch();
        } finally {
            RenderSystem.setProjectionMatrix(oldProjection, RenderSystem.getVertexSorting());
            RenderSystem.getModelViewStack().set(oldModelView);
            RenderSystem.applyModelViewMatrix();
            DEFERRED.clear();
        }
    }

    private record DeferredCore(GeoItemRenderer<WarpCore> renderer, GeoBone bone,
                                Matrix4f pose, Matrix3f normal,
                                Matrix4f modelView, Matrix4f projection,
                                int packedLight, int packedOverlay, int colour) {
    }
}
