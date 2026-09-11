package com.unddefined.enderechoing.client.renderer.entity;

import com.unddefined.enderechoing.client.model.entity.SculkZombieEntityModel;
import com.unddefined.enderechoing.entities.SculkZombieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SculkZombieEntityRenderer extends GeoEntityRenderer<SculkZombieEntity> {
    public SculkZombieEntityRenderer(EntityRendererProvider.Context c) {
        super(c, new SculkZombieEntityModel<>());
    }
}
