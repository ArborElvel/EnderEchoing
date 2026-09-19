package com.unddefined.enderechoing.mixin;

import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "dampensVibrations", at = @At("HEAD"), cancellable = true)
    private void dampensVibrations(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && SculkBorneBridge.hasVeil(player)) cir.setReturnValue(true);
    }
}
