package com.unddefined.enderechoing.client.model.item;

import com.unddefined.enderechoing.items.EnderEchoTuneChamber;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class EnderEchoTuneChamberModel<T extends EnderEchoTuneChamber> extends DefaultedItemGeoModel<T> {
    public EnderEchoTuneChamberModel() {super(ResourceLocation.fromNamespaceAndPath("enderechoing", "ender_echo_tune_chamber"));}

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {return RenderType.entityTranslucent(texture);}
}
