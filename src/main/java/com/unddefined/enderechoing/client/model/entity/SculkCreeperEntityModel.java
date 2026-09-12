package com.unddefined.enderechoing.client.model.entity;

import com.unddefined.enderechoing.client.model.cem.SculkCreeperCemAnimator;
import com.unddefined.enderechoing.entities.SculkCreeperEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SculkCreeperEntityModel<T extends SculkCreeperEntity> extends DefaultedEntityGeoModel<T> {
    private final SculkCreeperCemAnimator cemAnimator = new SculkCreeperCemAnimator(this);
    public SculkCreeperEntityModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "sculk_creeper"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        cemAnimator.apply(animatable, animationState);
    }
}
