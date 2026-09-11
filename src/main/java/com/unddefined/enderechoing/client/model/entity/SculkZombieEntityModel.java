package com.unddefined.enderechoing.client.model.entity;

import com.unddefined.enderechoing.entities.SculkZombieEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SculkZombieEntityModel<T extends SculkZombieEntity> extends DefaultedEntityGeoModel<T> {
    public SculkZombieEntityModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "sculk_zombie"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}

}
