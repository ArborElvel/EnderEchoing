package com.unddefined.enderechoing.client.model.block;

import com.unddefined.enderechoing.items.EnderEchoCrystal;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class EnderEchoCrystalModel<T extends EnderEchoCrystal> extends DefaultedItemGeoModel<T> {
    public EnderEchoCrystalModel() {super(ResourceLocation.fromNamespaceAndPath("enderechoing", "ender_echo_crystal"));}

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}
}
