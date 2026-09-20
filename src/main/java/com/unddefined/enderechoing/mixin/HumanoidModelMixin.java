package com.unddefined.enderechoing.mixin;

import com.unddefined.enderechoing.items.EnderEchoingCore;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 末影回响核心的使用动画。原版只为固定的几种 UseAnim 摆臂，
 * CUSTOM 需要模组自己实现，这里取代原先依赖 playerAnimator 的玩家动画层。
 * 使用状态本身由原版同步，因此其他玩家也能看到同样的举臂动作。
 * 姿态按 ender_echoing_core.player.use.animation.json 的两个关键帧复现：
 * 0.25s 帧抬到位并带一点内收/自转，之后线性收拢到 2.0s 帧。
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    // 0.25s 帧：两臂 -67.3885°，右臂 yRot -5.5418°、zRot -2.3033°（左臂取反）。
    @Unique private static final float USE_ARM_PEAK_X_ROT = -1.1761513F;
    @Unique private static final float USE_ARM_PEAK_Y_ROT = 0.09672245F;
    @Unique private static final float USE_ARM_PEAK_Z_ROT = 0.04019998F;
    // 2.0s 帧：两臂 -67.5°，yRot/zRot 归零。
    @Unique private static final float USE_ARM_HOLD_X_ROT = -1.1780972F;
    // 前 5 tick 抬到位，剩下 35 tick 收拢。
    @Unique private static final float USE_RAISE_TICKS = 5.0F;
    @Unique private static final float USE_SETTLE_TICKS = 35.0F;

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void enderechoing$poseEnderEchoingCoreArms(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                      float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty() || stack.getUseAnimation() != UseAnim.CUSTOM) return;
        if (!(stack.getItem() instanceof EnderEchoingCore)) return;
        
        float elapsed = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        float raise = Mth.clamp(elapsed / USE_RAISE_TICKS, 0.0F, 1.0F);
        float settle = Mth.clamp((elapsed - USE_RAISE_TICKS) / USE_SETTLE_TICKS, 0.0F, 1.0F);
        float xRot = Mth.lerp(settle, USE_ARM_PEAK_X_ROT, USE_ARM_HOLD_X_ROT);
        float yRot = Mth.lerp(settle, USE_ARM_PEAK_Y_ROT, 0.0F);
        float zRot = Mth.lerp(settle, USE_ARM_PEAK_Z_ROT, 0.0F);

        this.rightArm.xRot = Mth.lerp(raise, this.rightArm.xRot, xRot);
        this.rightArm.yRot = Mth.lerp(raise, this.rightArm.yRot, -yRot);
        this.rightArm.zRot = Mth.lerp(raise, this.rightArm.zRot, -zRot);
        this.leftArm.xRot = Mth.lerp(raise, this.leftArm.xRot, xRot);
        this.leftArm.yRot = Mth.lerp(raise, this.leftArm.yRot, yRot);
        this.leftArm.zRot = Mth.lerp(raise, this.leftArm.zRot, zRot);
    }
}
