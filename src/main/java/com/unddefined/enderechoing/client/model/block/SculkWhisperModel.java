package com.unddefined.enderechoing.client.model.block;

import com.unddefined.enderechoing.blocks.entity.SculkWhisperBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class SculkWhisperModel<T extends SculkWhisperBlockEntity> extends DefaultedBlockGeoModel<T> {
    public SculkWhisperModel() {
        super(ResourceLocation.fromNamespaceAndPath("enderechoing", "sculk_whisper"));
    }
}
