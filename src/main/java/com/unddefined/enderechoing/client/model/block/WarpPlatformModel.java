package com.unddefined.enderechoing.client.model.block;

import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class WarpPlatformModel<T extends WarpPlatformBlockEntity> extends DefaultedBlockGeoModel<T> {
    private static final ResourceLocation ASSET = ResourceLocation.fromNamespaceAndPath("enderechoing", "calibrated_sculk_shrieker");

    public WarpPlatformModel() {
        super(ASSET);
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return buildFormattedTexturePath(ASSET.withSuffix("_charged"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}

}
