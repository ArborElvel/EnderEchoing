package com.unddefined.enderechoing.client.renderer.layer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * AutoGlowingGeoLayer 的变体：把发光层提前到基础模型之前，并用独立缓冲区立即绘制。
 * <p>
 * 基础模型（例如包住 letters 的 shield/frame）会写深度，发光面在它之后绘制时会被深度测试整片剔除；
 * 而 1.21 的 MultiBufferSource 按 RenderType 复用共享批次、只在换类型或批次结束时 flush，
 * 同一件物品在同一帧被渲染多次（掉落的 chamber + tunerBlock 里的 chamber）时，
 * 发光批次可能被留成陈旧批次而始终不画出来。
 * 这里用自己的 ByteBufferBuilder 立即绘制：顺序固定为“发光层 → 基础模型 → core”，也不受共享批次状态影响。
 */
public class AutoGlowingBeforeModelLayer<T extends GeoAnimatable> extends AutoGlowingGeoLayer<T> {

    /** 发光 pass 会重画整个模型，需留够顶点与索引空间；批次结束即释放。 */
    private static final int GLOW_BUFFER_SIZE = 16384;

    public AutoGlowingBeforeModelLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                          MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                          int packedLight, int packedOverlay) {
        try (ByteBufferBuilder glowBuffer = new ByteBufferBuilder(GLOW_BUFFER_SIZE)) {
            MultiBufferSource.BufferSource glowSource = MultiBufferSource.immediate(glowBuffer);
            super.render(poseStack, animatable, bakedModel, renderType, glowSource, null, partialTick, packedLight, packedOverlay);
            glowSource.endBatch();
        }
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                       MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        // 发光层已在 preRender 中提前绘制，基础模型之后不再重复
    }
}
