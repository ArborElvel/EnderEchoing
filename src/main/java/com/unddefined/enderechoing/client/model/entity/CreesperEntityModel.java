package com.unddefined.enderechoing.client.model.entity;

import com.unddefined.enderechoing.client.model.cem.CreesperCemAnimator;
import com.unddefined.enderechoing.entities.CreesperEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CreesperEntityModel<T extends CreesperEntity> extends DefaultedEntityGeoModel<T> {
    private final CreesperCemAnimator cemAnimator = new CreesperCemAnimator(this);
    public CreesperEntityModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "creesper"));
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
