package com.unddefined.enderechoing.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import java.util.HashMap;
import java.util.Map;

public class WarpPlatformLayer extends BlockAndItemGeoLayer<WarpPlatformBlockEntity> {
    // 使用静态Map存储所有方块实体位置的状态，因为所有方块实体共享同一个Layer实例
    private static final Map<Long, Double> currentPositionYMap = new HashMap<>();
    private static final Map<Long, Float> currentRotationXMap = new HashMap<>();
    private static final Map<Long, Float> currentRotationZMap = new HashMap<>();
    private static final Map<Long, Float> currentPositionYAnimMap = new HashMap<>();

    public WarpPlatformLayer(GeoRenderer<WarpPlatformBlockEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, WarpPlatformBlockEntity animatable) {
        // 只在特定骨骼上渲染物品
        if (bone.getName().equals("item")) return new ItemStack(ItemRegistry.WARP_CORE.get());
        return this.stackForBone.apply(bone, animatable);
    }

    // 作用于单个BlockEntity
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, WarpPlatformBlockEntity animatable,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {

        BlockPos blockPos = animatable.getBlockPos();
        Vec3 centerPos = Vec3.atCenterOf(blockPos);
        long posKey = blockPos.asLong();

        // 如果方块实体已被移除或世界上该位置的方块实体不是当前实例，清理缓存，避免状态漂移
        var level = animatable.getLevel();
        if (level == null || level.getBlockEntity(blockPos) != animatable) {
            currentPositionYMap.remove(posKey);
            currentRotationXMap.remove(posKey);
            currentRotationZMap.remove(posKey);
            currentPositionYAnimMap.remove(posKey);
            return;
        }

        // 获取当前方块实体附近的最近玩家
        var nearestPlayer = level.getNearestPlayer(centerPos.x, centerPos.y, centerPos.z, 2.0, false);

        // 获取或初始化当前方块实体的位置和轴心Y值
        double currentPositionY = currentPositionYMap.getOrDefault(posKey, -0.18);

        // 根据是否有玩家来决定目标位置
        double targetPositionY = nearestPlayer == null ? -0.18 : 0.18;

        // 平滑过渡：每帧向目标位置靠近一点
        currentPositionY = Mth.lerp(0.05, currentPositionY, targetPositionY);

        // 更新Map中的值供下次使用
        currentPositionYMap.put(posKey, currentPositionY);

        // 应用骨骼动画（替代JSON动画）
        if (bone.getName().equals("item")) {
            // 获取动画时间
            float animTime = animatable.getAnimationTime();

            // 计算目标旋转值
            float targetRotationX = (float) (Math.sin(animTime * 0.04)) * 120;
            float targetRotationZ = (float) (Math.cos(animTime * 0.04)) * 120;

            // 计算目标位置值
            float targetPositionYAnim = (float) (Math.sin(animTime * 0.12) * 2);

            // 获取当前动画状态
            float currentRotationX = currentRotationXMap.getOrDefault(posKey, targetRotationX);
            float currentRotationZ = currentRotationZMap.getOrDefault(posKey, targetRotationZ);
            float currentPositionYAnim = currentPositionYAnimMap.getOrDefault(posKey, targetPositionYAnim);

            // 对动画值进行插值以获得平滑效果
            currentRotationX = Mth.lerp(0.1f, currentRotationX, targetRotationX);
            currentRotationZ = Mth.lerp(0.1f, currentRotationZ, targetRotationZ);
            currentPositionYAnim = Mth.lerp(0.1f, currentPositionYAnim, targetPositionYAnim);

            // 更新动画状态
            currentRotationXMap.put(posKey, currentRotationX);
            currentRotationZMap.put(posKey, currentRotationZ);
            currentPositionYAnimMap.put(posKey, currentPositionYAnim);

            // 应用到渲染
            poseStack.pushPose();
            // 移动到物品中心作为枢轴点
            poseStack.translate(0, 0.18, 0);

            // 应用动画位置变换
            poseStack.translate(0, currentPositionY + currentPositionYAnim / 16.0, 0);

            // 应用旋转变换（带插值）
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(currentRotationX), 1, 0, 0)));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(currentRotationZ), 0, 0, 1)));

            // 移回原位置
            poseStack.translate(0, -0.18, 0);

            // 渲染物品
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }
}