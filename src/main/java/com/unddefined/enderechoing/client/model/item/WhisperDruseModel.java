package com.unddefined.enderechoing.client.model.item;

import com.unddefined.enderechoing.items.WhisperDruse;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class WhisperDruseModel<T extends WhisperDruse> extends DefaultedItemGeoModel<T> {
    public WhisperDruseModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "whisper_druse"));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}
}
