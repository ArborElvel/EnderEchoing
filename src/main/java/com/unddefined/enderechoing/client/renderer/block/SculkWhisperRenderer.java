package com.unddefined.enderechoing.client.renderer.block;

import com.unddefined.enderechoing.blocks.entity.SculkWhisperBlockEntity;
import com.unddefined.enderechoing.client.model.block.SculkWhisperModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SculkWhisperRenderer extends GeoBlockRenderer<SculkWhisperBlockEntity> {
    public SculkWhisperRenderer() {
        super(new SculkWhisperModel<>());
    }
}
