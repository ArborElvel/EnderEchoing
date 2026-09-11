package com.unddefined.enderechoing.client.model.item;

import com.unddefined.enderechoing.items.EnderEchoingCore;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class EnderEchoingCoreModel<T extends EnderEchoingCore> extends DefaultedItemGeoModel<T> {
    public EnderEchoingCoreModel() {super(ResourceLocation.fromNamespaceAndPath("enderechoing", "ender_echoing_core"));}

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}
}
