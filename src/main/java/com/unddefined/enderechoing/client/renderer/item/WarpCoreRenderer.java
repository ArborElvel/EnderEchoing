package com.unddefined.enderechoing.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.enderechoing.client.model.item.WarpCoreModel;
import com.unddefined.enderechoing.client.renderer.layer.OutlineRenderer;
import com.unddefined.enderechoing.items.WarpCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

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

    public WarpCoreRenderer(GeoModel<WarpCore> model) {
        super(new WarpCoreModel<>());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, WarpCore animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender && bone.getName().equals("core"))
            buffer = ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(getTextureLocation(animatable)), true);

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
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

}
