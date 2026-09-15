package com.unddefined.enderechoing.client.renderer.entity;

import com.unddefined.enderechoing.client.model.entity.SculverfishEntityModel;
import com.unddefined.enderechoing.entities.SculverfishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SculverfishEntityRenderer extends GeoEntityRenderer<SculverfishEntity> {
    public SculverfishEntityRenderer(EntityRendererProvider.Context c) {
        super(c, new SculverfishEntityModel<>());
    }

    /**
     * 潜伏在幽匿块中时直接跳过模型和发光轮廓的渲染。
     *
     * <p>声波会让幽匿单位发光，而 GeoEntityRenderer 在发光时仍会画出轮廓；
     * 因此这里不依赖 {@code setInvisible()}，而是根据同步状态直接返回 {@code null}。
     */
    @Nullable
    @Override
    public RenderType getRenderType(SculverfishEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable.isBurrowed()) return null;
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public boolean shouldShowName(SculverfishEntity entity) {
        return !entity.isBurrowed() && super.shouldShowName(entity);
    }
}
