package com.unddefined.enderechoing.client.model.entity;

import com.unddefined.enderechoing.entities.SculkSpreaderEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SculkSpreaderEntityModel extends DefaultedEntityGeoModel<SculkSpreaderEntity> {
    private final ResourceLocation E = ResourceLocation.fromNamespaceAndPath("enderechoing", "sculk_spreader");
    public SculkSpreaderEntityModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "sculk_spreader"));
    }
    @Override
    public ResourceLocation getModelResource(SculkSpreaderEntity animatable) {return buildFormattedModelPath(E);}

    @Override
    public ResourceLocation getTextureResource(SculkSpreaderEntity animatable) {return buildFormattedTexturePath(E);}

    @Override
    public ResourceLocation getAnimationResource(SculkSpreaderEntity animatable) {return buildFormattedAnimationPath(E);}

    @Override
    public RenderType getRenderType(SculkSpreaderEntity animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}

}
