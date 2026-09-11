package com.unddefined.enderechoing.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.client.model.block.EnderEchoicResonatorModel;
import com.unddefined.enderechoing.client.renderer.layer.EnderEchoicResonatorLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class EnderEchoicResonatorRenderer extends GeoBlockRenderer<EnderEchoicResonatorBlockEntity> {
    public EnderEchoicResonatorRenderer() {
        super(new EnderEchoicResonatorModel<>());
        this.addRenderLayer(new EnderEchoicResonatorLayer(this));
    }
    @Override
    public void actuallyRender(PoseStack poseStack, EnderEchoicResonatorBlockEntity animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                               int packedOverlay, int colour) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

    }
}
