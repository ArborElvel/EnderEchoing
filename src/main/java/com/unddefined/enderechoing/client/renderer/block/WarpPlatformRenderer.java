package com.unddefined.enderechoing.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import com.unddefined.enderechoing.client.model.block.WarpPlatformModel;
import com.unddefined.enderechoing.client.renderer.layer.WarpPlatformLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static net.minecraft.client.renderer.LightTexture.FULL_BLOCK;

public class WarpPlatformRenderer extends GeoBlockRenderer<WarpPlatformBlockEntity> {
    private static final Minecraft mc = Minecraft.getInstance();
    public WarpPlatformRenderer() {
        super(new WarpPlatformModel<>());
        this.addRenderLayer(new WarpPlatformLayer(this));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, WarpPlatformBlockEntity animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                               int packedOverlay, int colour) {
        if (animatable.getSelectedPos() != GZERO) {
            poseStack.pushPose();
            // 渲染文本
            poseStack.translate(0, 1.6f, 0);
            var camera = mc.gameRenderer.getMainCamera();
            var name = animatable.getselectedName();
            float textWidth = mc.font.width(name) / 2.0f;
            poseStack.scale(0.033f, 0.033f, 0.033f);

            // 始终面向玩家
            poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            mc.font.drawInBatch(Component.literal(name), -textWidth, 0,
                    FastColor.ABGR32.color(255, 140, 244, 226), false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, FULL_BLOCK
            );
            poseStack.popPose();
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
