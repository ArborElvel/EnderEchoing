package com.unddefined.enderechoing.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.enderechoing.client.model.item.WarpCoreModel;
import com.unddefined.enderechoing.client.renderer.layer.OutlineRenderer;
import com.unddefined.enderechoing.items.WarpCore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.FastColor;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WarpCoreRenderer extends GeoItemRenderer<WarpCore> {

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

        OutlineRenderer.render(poseStack, model, "frame", 0.86F,
                FastColor.ABGR32.color(255, 90, 42, 77), 0.00F);
        OutlineRenderer.render(poseStack, model, "frame", 0.76F,
                FastColor.ABGR32.color(255, 117, 10, 237), 0.00F);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource,
                buffer, false, partialTick, packedLight, packedOverlay, colour);
    }

}
