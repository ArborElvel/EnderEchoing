package com.unddefined.enderechoing.client.model.item;

import com.unddefined.enderechoing.items.WarpCore;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class WarpCoreModel<T extends WarpCore> extends DefaultedItemGeoModel<T> {
    public WarpCoreModel() {super(ResourceLocation.fromNamespaceAndPath("enderechoing", "warp_core"));}

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}
}
