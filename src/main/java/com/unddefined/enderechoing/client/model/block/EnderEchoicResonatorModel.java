package com.unddefined.enderechoing.client.model.block;

import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.unddefined.enderechoing.blocks.EnderEchoTunerBlock.CHARGED;

public class EnderEchoicResonatorModel<T extends EnderEchoicResonatorBlockEntity> extends DefaultedBlockGeoModel<T> {
    private static final ResourceLocation ASSET = ResourceLocation.fromNamespaceAndPath("enderechoing", "calibrated_sculk_shrieker");

    public EnderEchoicResonatorModel() {
        super(ASSET);
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return buildFormattedTexturePath(animatable.getBlockState().getValue(CHARGED) ? ASSET.withSuffix("_charged")
                : ASSET);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}

}
