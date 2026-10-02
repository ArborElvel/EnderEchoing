package com.unddefined.enderechoing.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.unddefined.enderechoing.client.model.item.WarpCoreModel;
import com.unddefined.enderechoing.client.renderer.layer.OutlineRenderer;
import com.unddefined.enderechoing.client.renderer.layer.WarpCorePortalLayer;
import com.unddefined.enderechoing.client.shader.WarpCorePortalShaders;
import com.unddefined.enderechoing.compat.iris.IrisCompat;
import com.unddefined.enderechoing.items.WarpCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class WarpCoreRenderer extends GeoItemRenderer<WarpCore> {

    /** 外层描边：呼吸脉冲的周期（tick）。 */
    private static final int PULSE_PERIOD_TICKS = 30;
    /** 内层描边：外扩波纹的周期（tick），与回响波纹每 30 tick 一波的节奏一致。 */
    private static final int RIPPLE_PERIOD_TICKS = 30;
    /** 两个周期的最小公倍数：先把游戏时间取模再转 float，长时间运行也不会因精度丢失而抖动。 */
    private static final long CYCLE_TICKS = 120L;
    /** 当前实际渲染出的描边颜色（ARGB32 打包）。 */
    private static final int OUTER_OUTLINE_COLOR = FastColor.ARGB32.color(255, 90, 42, 77);
    private static final int INNER_OUTLINE_COLOR = FastColor.ARGB32.color(255, 117, 10, 237);

    /** 开光影时 core 的 UV 展开范围：1 表示把 core 的模型 UV 拉伸到整张 end_portal.png。 */
    private static final float PORTAL_UV_SPAN = 0.5F;
    /** 开光影时 core 的 UV 滚动周期（tick）：一个周期正好平移一整张 end_portal.png。 */
    private static final long PORTAL_SCROLL_PERIOD_TICKS = 200L;

    public WarpCoreRenderer(GeoModel<WarpCore> model) {
        super(new WarpCoreModel<>());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, WarpCore animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender && bone.getName().equals("core")) {
            // 开光影时 Iris 会跳过没接管的 shader：世界里的这一帧先记账，等 AFTER_LEVEL 再画
            if (WarpCorePortalLayer.shouldDefer(this.renderPerspective)) {
                WarpCorePortalLayer.enqueue(this, bone, poseStack,
                        RenderSystem.getProjectionMatrix(), RenderSystem.getModelViewMatrix(),
                        packedLight, packedOverlay, colour);
                return;
            }
            renderCore(poseStack, bone, bufferSource, packedLight, packedOverlay, colour, partialTick);
            return;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /**
     * core 骨自己画：基础层 + （用自绘星云 shader 时）绕立方体中心放大一圈的加法光晕。
     * 这里覆盖了 GeckoLib 的默认渲染，所以要手动补上骨骼变换，并且不再走 super。
     */
    private void renderCore(PoseStack poseStack, GeoBone bone, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay, int colour, float partialTick) {
        poseStack.pushPose();
        try {
            RenderUtil.prepMatrixForBone(poseStack, bone);
            this.renderCubesOfBone(poseStack, bone, coreBuffer(bufferSource, bone, partialTick),
                    packedLight, packedOverlay, colour);

            if (usesPortalShader()) {
                if (bufferSource instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch(WarpCorePortalLayer.RENDER_TYPE);
                }
                WarpCorePortalLayer.renderGlow(this, poseStack, bone, bufferSource, packedLight, packedOverlay, colour);
                if (bufferSource instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch(WarpCorePortalLayer.GLOW_TYPE);
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    /** 把描边色作为星云染色传给自绘 shader：core 的星云与描边同色系。 */
    public static void applyPortalTint() {
        WarpCorePortalShaders.setTintColor(
                FastColor.ARGB32.red(OUTER_OUTLINE_COLOR) / 255F,
                FastColor.ARGB32.green(OUTER_OUTLINE_COLOR) / 255F,
                FastColor.ARGB32.blue(OUTER_OUTLINE_COLOR) / 255F);
    }

    /** 基础层是否用的是自绘星云 shader（决定要不要叠光晕）。 */
    private boolean usesPortalShader() {
        return IrisCompat.isShaderPackInUse() && WarpCorePortalShaders.isReady()
                && this.renderPerspective == ItemDisplayContext.GUI;
    }

    /**
     * core 骨的顶点缓冲。
     * <p>
     * 无光影时用原版 {@link RenderType#endPortal()}，它的 shader 会按顶点位置投影 UV。
     * 开光影时不能再用：Iris/Oculus 只接管 shader 包提供的程序，不接管 rendertype_end_portal，
     * 未接管的原版 shader 会被 DepthColorStorage 直接关掉 colorMask 和 depthMask。
     * GUI 里不在 world 渲染阶段，可以安全地用我们自己的 core shader 画星云；
     * 其余场景（掉落物、展示框等）退回 shader 包认识的 entitySolid + end_portal.png 兜底。
     */
    private VertexConsumer coreBuffer(MultiBufferSource bufferSource, GeoBone core, float partialTick) {
        if (!IrisCompat.isShaderPackInUse())
            return bufferSource.getBuffer(RenderType.endPortal());

        if (WarpCorePortalShaders.isReady() && this.renderPerspective == ItemDisplayContext.GUI) {
            applyPortalTint();
            return bufferSource.getBuffer(WarpCorePortalLayer.RENDER_TYPE);
        }

        return new EndPortalLikeConsumer(
                bufferSource.getBuffer(RenderType.entitySolid(TheEndPortalRenderer.END_PORTAL_LOCATION)),
                portalScroll(partialTick), uvWindowOf(core));
    }

    /**
     * core 的模型 UV 只占 end_portal.png 上很小的一块（8x8 贴图的 6..8 像素，即 0.25 宽）。
     * 开光影时把这块窗口线性拉伸到 {@link #PORTAL_UV_SPAN} 这么大，滚动时才能看到完整花纹。
     */
    private static UvWindow uvWindowOf(GeoBone bone) {
        float minU = Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE;
        float maxV = -Float.MAX_VALUE;
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices()) {
                    minU = Math.min(minU, vertex.texU());
                    minV = Math.min(minV, vertex.texV());
                    maxU = Math.max(maxU, vertex.texU());
                    maxV = Math.max(maxV, vertex.texV());
                }
            }
        }
        if (minU > maxU || minV > maxV) return UvWindow.FULL;

        float spanU = maxU - minU;
        float spanV = maxV - minV;
        return new UvWindow(minU, minV,
                spanU < 1.0E-4F ? 1F : PORTAL_UV_SPAN / spanU,
                spanV < 1.0E-4F ? 1F : PORTAL_UV_SPAN / spanV);
    }

    /** 开光影时 core 的 UV 滚动：按游戏 tick 平移，先取模再转 float，长时间运行不丢精度。 */
    private static float portalScroll(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) return 0F;
        return ((level.getGameTime() % PORTAL_SCROLL_PERIOD_TICKS) + partialTick) / PORTAL_SCROLL_PERIOD_TICKS;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, WarpCore animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {

        if (isReRender) return;
        if (bufferSource instanceof MultiBufferSource.BufferSource source) source.endBatch();

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource,
                buffer, false, partialTick, packedLight, packedOverlay, colour);

        // 描边画在基础模型之后：外壳和核心写下的深度都比描边更近，会把描边挡在它们后面，
        // 于是描边（只画背面那层）在核心周围露出一圈光而不会盖住核心；更近的方块/实体照常遮挡描边，
        // 描边自己写深度也会挡住它后面的实体。
        // flush 一次保证模型顶点先落到 GPU，否则描边会先画、再被随后 flush 的模型盖回去。
        if (bufferSource instanceof MultiBufferSource.BufferSource source) source.endBatch();

        float time = outlineTime(partialTick);

        // 外层：呼吸脉冲——明暗起伏为主，缩放只做极小幅度的扩张收缩。
        float pulse = (Mth.sin(time * (float) (Math.PI * 2) / PULSE_PERIOD_TICKS) + 1F) * 0.5F;
        OutlineRenderer.render(poseStack, model, "frame",
                0.84F + 0.015F * (pulse * 2F - 1F),
                modulateRgb(OUTER_OUTLINE_COLOR, 0.60F + 0.55F * pulse), 0.00F);

        // 内层：外扩波纹——每周期从基准位置向外扩一圈并淡出，亮度归零后再从头开始，所以看不到跳变。
        float progress = (time % RIPPLE_PERIOD_TICKS) / RIPPLE_PERIOD_TICKS;
        float fade = ((1F - progress) * (1F - progress));
        OutlineRenderer.render(poseStack, model, "frame",
                0.74F + 0.12F * progress,
                modulateRgb(INNER_OUTLINE_COLOR, fade * 1.2f), 0.00F);
    }

    /**
     * 描边动效的时间基准：游戏 tick 加当前帧的部分 tick。
     * 先按两个周期的最小公倍数取模再转 float，长时间运行也不会因 float 精度丢失而抖动；
     * 尚未进入世界（加载界面等）时返回 0，描边退化为静态。
     */
    private static float outlineTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) return 0F;
        return (level.getGameTime() % CYCLE_TICKS) + partialTick;
    }

    /**
     * 按系数缩放 RGB 亮度。
     * 描边用 GLINT_TRANSPARENCY（加法混合，结果只看 RGB 不看 alpha），所以淡入淡出必须缩放颜色通道而不是 alpha。
     */
    private static int modulateRgb(int argb, float factor) {
        int red = Mth.clamp(Math.round(FastColor.ARGB32.red(argb) * factor), 0, 255);
        int green = Mth.clamp(Math.round(FastColor.ARGB32.green(argb) * factor), 0, 255);
        int blue = Mth.clamp(Math.round(FastColor.ARGB32.blue(argb) * factor), 0, 255);
        return FastColor.ARGB32.color(255, red, green, blue);
    }

    /** core 骨 UV 的线性重映射参数：u/v 分别减去 base 再按 scale 缩放，最后叠加滚动偏移。 */
    private record UvWindow(float baseU, float baseV, float scaleU, float scaleV) {
        private static final UvWindow FULL = new UvWindow(0F, 0F, 1F, 1F);
    }

    /**
     * 只做三件事的顶点转发器：把 UV 重映射到整张贴图并加滚动偏移、固定颜色、把光照固定为满亮度。
     * GeckoLib 走的是 {@link VertexConsumer#addVertex(float, float, float, int, float, float, int, int, float, float, float)}
     * 这条默认方法，它内部会回调下面重写的各个 setter，所以不需要额外重写。
     */
    private record EndPortalLikeConsumer(VertexConsumer delegate, float scroll, UvWindow window) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            // 末地传送门贴图本身就是「黑底 + 亮星点」，再乘暗色只会变成一团紫黑；
            // 这里原样透传物品的白颜色，让星点保持亮度和颜色。
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv((u - window.baseU()) * window.scaleU() + scroll,
                    (v - window.baseV()) * window.scaleV() + scroll);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            // 原版 rendertype_end_portal 的顶点格式里没有 lightmap，观感始终是自发光；
            // entitySolid 有 lightmap，这里固定满亮度来还原。
            delegate.setUv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >> 16 & 0xFFFF);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }

}
