package com.unddefined.enderechoing.client.model.block;

import com.unddefined.enderechoing.blocks.entity.EnderEchoCrystalBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class EnderEchoCrystalBlockModel<T extends EnderEchoCrystalBlockEntity> extends DefaultedBlockGeoModel<T> {
    public EnderEchoCrystalBlockModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "calibrated_sculk_shrieker"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}
}
