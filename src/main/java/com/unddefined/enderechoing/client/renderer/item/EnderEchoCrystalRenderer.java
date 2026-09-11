package com.unddefined.enderechoing.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.enderechoing.client.model.block.EnderEchoCrystalModel;
import com.unddefined.enderechoing.items.EnderEchoCrystal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EnderEchoCrystalRenderer extends GeoItemRenderer<EnderEchoCrystal> {
    public EnderEchoCrystalRenderer() {
        super(new EnderEchoCrystalModel<>());
    }
    private final ResourceLocation Core_layer = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/misc/core_layer.png");
    @Override
    public void renderRecursively(PoseStack poseStack, EnderEchoCrystal animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender && bone.getName().equals("core")) {
            renderType = RenderType.entitySolid(Core_layer);
            buffer = bufferSource.getBuffer(renderType);
            packedLight = 0xF000F0;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,partialTick, packedLight, packedOverlay,colour);
    }
}
