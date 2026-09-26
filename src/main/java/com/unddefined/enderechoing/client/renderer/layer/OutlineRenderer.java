package com.unddefined.enderechoing.client.renderer.layer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

/**
 * GeckoLib Geo 模型的几何描边：顶点沿面法线外扩。
 * <p>
 * 只画几何的背面（顶点绕序反转 + CULL），近侧表面被剔除：描边因此不会盖住它包着的核心，
 * 而是从外壳的镂空里露出一圈贴着核心的光。
 * <p>
 * 必须在基础模型之后调用，并且调用前先把基础模型的批次 flush 掉：此时模型已写入深度，
 * 比描边更近的外壳/核心会把描边挡掉，比描边更远的方块和实体也会被描边挡住（描边写深度），
 * 前后遮挡都交给正常的深度测试，不需要关掉深度测试（关掉反而会被自己的模型按绘制顺序盖住）。
 */
public final class OutlineRenderer {

    private OutlineRenderer() {
    }
    public static void render(PoseStack poseStack, BakedGeoModel model, String targetBone, float scale, int color, float offset) {
        ByteBufferBuilder outlineBuilder = new ByteBufferBuilder(256);
        try (outlineBuilder) {
            var outlineSource = MultiBufferSource.immediate(outlineBuilder);
            var outlineBuffer = outlineSource.getBuffer(outlineRenderType);
            render(poseStack, model, outlineBuffer, targetBone, scale, color, offset);
            outlineSource.endBatch();
        }
    }

    public static void render(PoseStack poseStack, BakedGeoModel model, VertexConsumer consumer, String targetBone,
                              float scale, int color, float offset) {
        for (GeoBone bone : model.topLevelBones()) {
            GeoBone target = findBone(bone, targetBone);
            if (target != null) {
                poseStack.pushPose();
                try {
                    // warp_core 的模型中心约在 Y=3/16；绕中心放大，避免整体向上偏移。
                    poseStack.translate(0f, 3f / 16f, 0f);
                    poseStack.scale(scale, scale, scale);
                    poseStack.translate(0f, -3f / 16f, 0f);
                    renderBone(poseStack, target, consumer, color, offset);
                } finally {
                    poseStack.popPose();
                }
                return;
            }
        }
    }

    private static GeoBone findBone(GeoBone bone, String name) {
        if (bone.getName().equals(name)) return bone;
        for (GeoBone child : bone.getChildBones()) {
            GeoBone result = findBone(child, name);
            if (result != null) return result;
        }
        return null;
    }

    private static void renderBone(PoseStack poseStack, GeoBone bone, VertexConsumer consumer, int color, float offset) {
        if (bone.isHidden()) return;
        poseStack.pushPose();
        try {
            RenderUtil.prepMatrixForBone(poseStack, bone);

            for (GeoCube cube : bone.getCubes()) renderCube(poseStack, cube, consumer, color, offset);

            if (!bone.isHidingChildren())
                for (GeoBone child : bone.getChildBones()) renderBone(poseStack, child, consumer, color, offset);

        } finally {
            poseStack.popPose();
        }
    }

    private static void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer consumer, int color, float offset) {
        poseStack.pushPose();
        try {
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);

            Matrix4f matrix = new Matrix4f(poseStack.last().pose());
            Vector3f normal = new Vector3f();

            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;

                // 外扩必须使用模型空间法线；变换后的法线只用于写入顶点属性。
                Vector3f localNormal = new Vector3f(quad.normal()).normalize();
                normal.set(localNormal);
                poseStack.last().normal().transform(normal).normalize();

                // 逆序写入：GeckoLib 的 quad 是按"从外侧看逆时针"排的正面，
                // 反过来配合 CULL 就只留下背面那一层，描边才不会糊在核心正面。
                GeoVertex[] vertices = quad.vertices();
                for (int i = vertices.length - 1; i >= 0; i--) {
                    GeoVertex vertex = vertices[i];
                    Vector3f position = new Vector3f(vertex.position());
                    position.fma(offset, localNormal);
                    matrix.transformPosition(position);

                    consumer.addVertex(position.x(), position.y(), position.z()).setColor(color)
                            .setUv(vertex.texU(),vertex.texV()).setUv1(0,1).setUv2(2,0)
                            .setNormal(normal.x, normal.y, normal.z);
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    public static RenderType outlineRenderType = RenderType.create(
            "enderechoing_outline",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    // 背面剔除配合上面反转的顶点绕序：只画远离相机的那层几何
                    .setCullState(RenderStateShard.CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    // 写深度：描边要能挡住它后面的实体，否则后画的实体还会从描边里透出来
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .createCompositeState(false)
    );

}
