package com.unddefined.enderechoing.client.model.entity;

import com.unddefined.enderechoing.entities.SculkSpreaderEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SculkSpreaderEntityModel<T extends SculkSpreaderEntity> extends DefaultedEntityGeoModel<T> {
    public SculkSpreaderEntityModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "sculk_spreader"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}

}
