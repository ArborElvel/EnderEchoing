package com.unddefined.enderechoing.client.renderer.item;

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

import static net.minecraft.client.renderer.LightTexture.FULL_BLOCK;

public class EnderEchoingCoreRenderer extends GeoItemRenderer<EnderEchoingCore> {
    public EnderEchoingCoreRenderer(GeoModel<EnderEchoingCore> model) {
        super(new EnderEchoingCoreModel<>());
    }
    private final ResourceLocation Core_layer = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/misc/core_layer.png");
    @Override
    public void renderRecursively(PoseStack poseStack, EnderEchoingCore animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender && bone.getName().equals("core")) {
            renderType = RenderType.entitySolid(Core_layer);
            buffer = bufferSource.getBuffer(renderType);
            packedLight = FULL_BLOCK;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,partialTick, packedLight, packedOverlay,colour);
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
