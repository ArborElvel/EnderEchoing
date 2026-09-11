package com.unddefined.enderechoing.client.model.entity;

import com.unddefined.enderechoing.entities.EnderEchoCrystalEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class EnderEchoCrystalEntityModel<T extends EnderEchoCrystalEntity> extends DefaultedEntityGeoModel<T> {
    public EnderEchoCrystalEntityModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "ender_echo_crystal"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}

}
