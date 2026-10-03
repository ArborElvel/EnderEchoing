package com.unddefined.enderechoing.client.particles;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.unddefined.enderechoing.ClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.function.Function;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

@OnlyIn(Dist.CLIENT)
public class EchoResponding {
    private static final Function<ResourceLocation, RenderType> RESPONDING = Util.memoize(r-> RenderType.create(
            "ender_echoic_responsing",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(r, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true)
    ));
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos blockPos, int ticks) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double screenHeight = Minecraft.getInstance().getWindow().getHeight();
        double offX = blockPos.getCenter().x - camera.getPosition().x;
        double offY = blockPos.getCenter().y - camera.getPosition().y + 1;
        double offZ = blockPos.getCenter().z - camera.getPosition().z;
        poseStack.pushPose();
        double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
        if (distance > 250000.0D) {
            double offScaler = 250000.0D / distance;
            offX *= offScaler;
            offY *= offScaler;
            offZ *= offScaler;
        }
        poseStack.translate(offX, offY, offZ);
        // apply camera-facing rotation so the quad faces the camera
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        // scale to screen-constant size
        double fov = Minecraft.getInstance().options.fov().get().doubleValue();
        double fovMultiplier = 2.0D * Math.tan(Math.toRadians(fov / 2.0D));
        float screenScale = (float) (distance / screenHeight / fovMultiplier * 160);
        if (screenScale < 1) screenScale = 1.0f;
        poseStack.scale(screenScale, screenScale, 0);

        // 渲染SONIC_BOOM
        poseStack.pushPose();
        int frameCount = 14;
        int frameDuration = 3;
        int frameIndex = frameCount - 1 - (ticks / frameDuration) % frameCount;
        VertexConsumer vc = bufferSource.getBuffer(RESPONDING.apply(
                ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/misc/sonic_boom_" + frameIndex + ".png")));
            int color = ClientConfig.abgr(ClientConfig.ECHO_RESPONSE_COLOR, 255);
        Matrix4f mat = poseStack.last().pose();
        // 绘制一个平面 quad，包含所有必需的顶点属性
        vc.addVertex(mat, -1f, -1f, 0f).setUv(0f, 0f).setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
        vc.addVertex(mat, 1f, -1f, 0f).setUv(1f, 0f).setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
        vc.addVertex(mat, 1f, 1f, 0f).setUv(1f, 1f).setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
        vc.addVertex(mat, -1f, 1f, 0f).setUv(0f, 1f).setColor(color)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
        poseStack.popPose();

        // pop world pose for quad drawing
        poseStack.popPose();
    }
}
