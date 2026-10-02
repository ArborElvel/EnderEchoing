package com.unddefined.enderechoing.client.renderer.item;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.enderechoing.client.model.item.EnderEchoTuneChamberModel;
import com.unddefined.enderechoing.client.renderer.layer.AutoGlowingBeforeModelLayer;
import com.unddefined.enderechoing.items.EnderEchoTuneChamber;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class EnderEchoTuneChamberRenderer extends GeoItemRenderer<EnderEchoTuneChamber> {
    public EnderEchoTuneChamberRenderer() {
        super(new EnderEchoTuneChamberModel<>());
        // letters 的发光面被 shield/frame 包住，必须在外壳写下深度之前绘制，否则会被深度测试整片剔除
        addRenderLayer(new AutoGlowingBeforeModelLayer<>(this));
    }
    private final ResourceLocation Core_layer = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/misc/core_layer.png");

    /**
     * 绘制顺序固定为 core → 发光层 → 基础外壳。
     * <p>
     * core 同样被 shield/frame 包住，若留在基础模型 pass 里，换 RenderType 会先把外壳批次画掉，
     * core 再画时会被外壳写下的深度整片剔除（1.21 的共享 MultiBufferSource 同时只保留一个批次；
     * Iris 的分组渲染把不透明批次排在前面，恰好掩盖了这个问题）。
     * 所以 core 必须在外壳之前，用自己的缓冲区立即绘制。
     * <p>
     * 这里留在 preRender 是因为调谐腔的 core 是顶层骨骼、模型没有动画，自身变换即可定位；
     * 同时它必须排在发光层（letters 的 glowmask，写在 preRender 里）之前，否则不透明的 core
     * 会把发光盖掉。回响核心 / 回响晶簇的 core 是动画骨骼的子节点，改用 traversal 内绘制
     * （父级姿势与实体朝向要等 actuallyRender 才算好，提前画会晚一帧）。
     */
    @Override
    public void preRender(PoseStack poseStack, EnderEchoTuneChamber animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // 发光层会 reRender 整个模型，此时 core 已画过，不再重复
        if (isReRender) return;

        model.getBone("core").ifPresent(coreBone -> {
            // core 是单个 6 面方块，独立批次结束即释放
            try (ByteBufferBuilder coreBufferBuilder = new ByteBufferBuilder(4096)) {
                MultiBufferSource.BufferSource coreSource = MultiBufferSource.immediate(coreBufferBuilder);

                poseStack.pushPose();
                RenderUtil.prepMatrixForBone(poseStack, coreBone);
                renderCubesOfBone(poseStack, coreBone, coreSource.getBuffer(RenderType.entitySolid(Core_layer)), 0xF000F0, packedOverlay, colour);
                poseStack.popPose();

                coreSource.endBatch();
            }
        });
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EnderEchoTuneChamber animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        // core 已由 preRender 在外壳之前画好，基础模型 pass 里跳过，避免被外壳纹理重画
        if (!isReRender && bone.getName().equals("core")) return;

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
