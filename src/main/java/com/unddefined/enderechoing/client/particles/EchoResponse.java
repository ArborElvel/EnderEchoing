package com.unddefined.enderechoing.client.particles;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.unddefined.enderechoing.client.renderer.EchoRenderer.EchoSoundingPos;
import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

@OnlyIn(Dist.CLIENT)
public class EchoResponse {
    public static final RenderType WAVE_RENDER_TYPE = RenderType.create(
            "ender_echoic_wave",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/misc/wave.png"), false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true)
    );
    private final List<Integer> activeWaves = new ArrayList<>();
    private final BlockPos unShiftPos;
    public int hoveringTicks = -31;
    public Boolean isElementHovering = false;
    private Boolean Shifted = false;
    private BlockPos blockPos;
    private BlockPos targetPos = null;

    public EchoResponse(BlockPos pos) {
        this.blockPos = pos;
        unShiftPos = pos;
    }


    public boolean render(Player player, PoseStack poseStack, MultiBufferSource bufferSource, int ticks, boolean isCountingDown, String posName) {
        if (player.isShiftKeyDown() && EchoSoundingPos != null && !Shifted) {
            // 使用 Shift 键触发Y随机偏移，以避免多个锚点渲染重叠
            int shiftInt = Math.max(blockPos.distManhattan(EchoSoundingPos), 6);
            blockPos = new BlockPos(blockPos.above(player.getRandom().nextInt(shiftInt) - shiftInt / 2));
            Shifted = true;
        }
        if (!player.isShiftKeyDown()) {
            blockPos = unShiftPos;
            Shifted = false;
        }
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double offX = blockPos.getCenter().x - camera.getPosition().x;
        double offY = blockPos.getCenter().y - camera.getPosition().y + 1;
        double offZ = blockPos.getCenter().z - camera.getPosition().z;
        double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
        // 把渲染点拉到相机前 128 米以内，避免 float 精度坍塌造成“中心消失/边缘能显示”的裁剪现象。
        final double MAX_RENDER_DISTANCE = 128;
        final double MIN_RENDER_DISTANCE = 2.0;
        double renderDistance;
        var dir = new Vector3d(offX, offY, offZ).normalize();
        renderDistance = Math.min(distance, MAX_RENDER_DISTANCE);
        renderDistance = Math.max(renderDistance, MIN_RENDER_DISTANCE);
        double rx = dir.x * renderDistance;
        double ry = dir.y * renderDistance;
        double rz = dir.z * renderDistance;
        poseStack.pushPose();
        poseStack.translate((float) rx, (float) ry, (float) rz);

        // 应用相机朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        // 固定大小
        float screenScale = 0.06f * (float) Math.min(distance, MAX_RENDER_DISTANCE);
        if (screenScale < 1f) screenScale = 1f;
        poseStack.scale(screenScale, screenScale, 1f);
        // 当玩家准星看向那个位置时1高亮波纹，并使得波纹由大变小
        var dir2 = new Vector3f((float) (offX / distance), (float) (offY / distance), (float) (offZ / distance));
        isElementHovering = camera.getLookVector().dot(dir2) >= 0.999f;
        if (isElementHovering) {
            targetPos = blockPos;
            ticks = hoveringTicks;
            if (!activeWaves.contains(-30)) {
                activeWaves.clear();
                activeWaves.add(10);
                activeWaves.add(40);
            }
        } else if (targetPos != null && targetPos.equals(blockPos)) hoveringTicks = -34;

        if (!isElementHovering) {
            // 每隔30tick生成一个新的波纹
            if (ticks < 0 && ticks % 30 == 0) activeWaves.add(-ticks);
            else if (!isCountingDown && ticks % 30 == 0) activeWaves.add(ticks);
        }

        // 渲染三个波纹
        var it = activeWaves.iterator();
        while (it.hasNext()) {
            int age = ticks - it.next(); // 周期性扩散
            if (age > 120) {
                it.remove(); // 生命周期结束
                continue;
            }
            float scale2 = Math.abs(0.2f + age / 80f); // 控制半径增大
            float alpha = Math.max(0f, 0.9f - age / 90f); // 随半径增大透明度逐渐减小
            poseStack.pushPose();
            poseStack.scale(scale2, scale2, 0); // 缩放波纹平面
            if (bufferSource == null) {
                poseStack.popPose();
                continue;
            }
            var vertexConsumer = bufferSource.getBuffer(WAVE_RENDER_TYPE);
            var matrix4f = poseStack.last().pose();
            int color = isElementHovering ? FastColor.ABGR32.color((int) (alpha * 255), 140, 244, 226)
                    : FastColor.ABGR32.color((int) (alpha * 255 * Math.max(0.1, (1 - (distance / 4096)))), 44, 205, 177);
            // 绘制一个平面 quad，包含所有必需的顶点属性
            vertexConsumer.addVertex(matrix4f, -1f, -1f, 0f).setUv(0f, 0f).setColor(color)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
            vertexConsumer.addVertex(matrix4f, 1f, -1f, 0f).setUv(1f, 0f).setColor(color)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
            vertexConsumer.addVertex(matrix4f, 1f, 1f, 0f).setUv(1f, 1f).setColor(color)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
            vertexConsumer.addVertex(matrix4f, -1f, 1f, 0f).setUv(0f, 1f).setColor(color)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
            poseStack.popPose();
        }

        // 渲染位置名称
        if (bufferSource != null && posName != null && ticks > 0) {
            poseStack.pushPose();
            // 渲染文本
            Font font = Minecraft.getInstance().font;
            float textWidth = font.width(posName) / 2.0f;
            poseStack.translate(0, -0.13f, 0);
            poseStack.scale(0.033f, 0.033f, 0.033f);
            font.drawInBatch(
                    Component.literal(posName),
                    -textWidth,
                    0,
                    FastColor.ABGR32.color(255, 140, 244, 226),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    FULL_BRIGHT
            );
            poseStack.popPose();
        }
        poseStack.popPose();
        return isElementHovering;
    }
}
