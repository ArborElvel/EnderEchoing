package com.unddefined.enderechoing.client.renderer.item;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.enderechoing.client.model.item.EnderEchoingCoreModel;
import com.unddefined.enderechoing.items.EnderEchoingCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import static net.minecraft.client.renderer.LightTexture.FULL_BLOCK;

public class EnderEchoingCoreRenderer extends GeoItemRenderer<EnderEchoingCore> {
    public EnderEchoingCoreRenderer(GeoModel<EnderEchoingCore> model) {
        super(new EnderEchoingCoreModel<>());
    }
    private final ResourceLocation Core_layer = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/misc/core_layer.png");

    @Override
    public void renderRecursively(PoseStack poseStack, EnderEchoingCore animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender && bone.getName().equals("core")) {
            renderCore(poseStack, bone, packedOverlay, colour);
            return;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /**
     * core 骨自己画。
     * <p>
     * 外壳（frame/cube）会写深度：留在基础 pass 里换 RenderType 会先把外壳批次画掉，
     * core 再画就被外壳深度整片剔除（1.21 的共享 MultiBufferSource 同时只保留一个批次；
     * Iris 的分组渲染把不透明批次排在前面，恰好掩盖了这个问题）。
     * 这里用独立缓冲区立即出图，共享批次里的外壳保持待画，随后才混合盖上来。
     * <p>
     * 必须留在 traversal 里而不是 preRender：core 是动画骨骼（bone/cube）的子节点，
     * 父级姿势要等 actuallyRender 里 handleAnimations 跑完才准确，提前画会用到上一帧的姿势。
     */
    private void renderCore(PoseStack poseStack, GeoBone bone, int packedOverlay, int colour) {
        // core 是单个 6 面方块，独立批次结束即释放
        try (ByteBufferBuilder coreBufferBuilder = new ByteBufferBuilder(4096)) {
            MultiBufferSource.BufferSource coreSource = MultiBufferSource.immediate(coreBufferBuilder);

            poseStack.pushPose();
            RenderUtil.prepMatrixForBone(poseStack, bone);
            renderCubesOfBone(poseStack, bone, coreSource.getBuffer(RenderType.entitySolid(Core_layer)), FULL_BLOCK, packedOverlay, colour);
            poseStack.popPose();

            coreSource.endBatch();
        }
    }

    protected void renderInGui(ItemDisplayContext transformType, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick) {

        this.animatable = (EnderEchoingCore) this.getCurrentItemStack().getItem();
        this.currentItemStack = this.getCurrentItemStack();
        this.renderPerspective = transformType;
        
        // 只在播放使用动画时应用平移
        ItemStack activeStack = Minecraft.getInstance().player.getUseItem();
        if (this.currentItemStack != null && activeStack.getItem() instanceof EnderEchoingCore &&
                Minecraft.getInstance().player.isUsingItem()) {
            poseStack.translate(0.3, 0.26, 0);
        }
        RenderType renderType = getRenderType(this.animatable, getTextureLocation(this.animatable), bufferSource, partialTick);
        VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, this.currentItemStack != null && this.currentItemStack.hasFoil());

        defaultRender(poseStack, this.animatable, bufferSource, renderType, buffer, 0f, partialTick, packedLight);

    }
}
