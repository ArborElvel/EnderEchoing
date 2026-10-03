package com.unddefined.enderechoing.client.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.unddefined.enderechoing.client.renderer.EchoRenderer;
import com.unddefined.enderechoing.client.particles.EchoResponding;
import com.unddefined.enderechoing.client.renderer.layer.OutlineRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * 客户端渲染工具入口。
 *
 * <p>现在的工具都来自模组自身的渲染实现，签名保持稳定；调用方自行负责渲染时机。
 */
public final class EnderEchoClientRender {
    private EnderEchoClientRender() {
    }

    /**
     * 给 GeckoLib Geo 模型的指定骨骼画几何描边（顶点沿面法线外扩）。
     *
     * <p>必须在基础模型之后调用，并在调用前 flush 掉基础模型的批次。描边会写深度，
     * 前后遮挡交给正常的深度测试，不要关掉深度测试。
     *
     * <p>放大中心按 {@code Y = 3/16} 补偿，与回响核心 / 水晶的模型一致；
     * 你的模型中心不在这里时，需要自己在 {@code poseStack} 上先做平移。
     *
     * @param boneName 目标骨骼名，找不到时什么都不画
     * @param scale    放大倍数
     * @param color    ARGB 颜色
     * @param offset   沿法线外扩的偏移量
     */
    public static void drawBoneOutline(PoseStack poseStack, BakedGeoModel model, String boneName,
                                       float scale, int color, float offset) {
        OutlineRenderer.render(poseStack, model, boneName, scale, color, offset);
    }

    /**
     * 在指定方块位置画一圈回响响应波纹。
     *
     * <p>波纹以该方块中心为原点，位置按「相对相机」计算，所以在第三人称与旁观模式下同样正确。
     *
     * @param blockPos 波纹中心
     * @param ticks    推进刻度，用来控制扩散进度
     */
    public static void drawEchoResponse(PoseStack poseStack, MultiBufferSource bufferSource,
                                        BlockPos blockPos, int ticks) {
        EchoResponding.render(poseStack, bufferSource, blockPos, ticks);
    }

    /**
     * 在当前 {@code RenderLevelStageEvent} 里补画回响波。
     *
     * <p>给「也用 AFTER_LEVEL 往主渲染目标画东西」的模组用：在自己的后处理画完之后调用，
     * 让回响波固定叠在你的效果之上；同一帧内重复调用不会画第二遍。非 {@code AFTER_LEVEL}
     * 阶段调用无效。
     */
    public static void renderEchoWaveAfterVeil(RenderLevelStageEvent event) {
        EchoRenderer.renderEchoAfterSculkVeil(event);
    }
}
