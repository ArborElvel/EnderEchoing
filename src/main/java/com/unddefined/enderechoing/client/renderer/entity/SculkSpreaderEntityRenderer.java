package com.unddefined.enderechoing.client.renderer.entity;

import com.unddefined.enderechoing.client.model.entity.SculkSpreaderEntityModel;
import com.unddefined.enderechoing.entities.SculkSpreaderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SculkSpreaderEntityRenderer  extends GeoEntityRenderer<SculkSpreaderEntity> {
    public SculkSpreaderEntityRenderer(EntityRendererProvider.Context c) {
        super(c, new SculkSpreaderEntityModel());
    }
}
