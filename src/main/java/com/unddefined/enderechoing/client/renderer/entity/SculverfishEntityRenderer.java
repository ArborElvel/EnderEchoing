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
     * 潜伏在幽匿块中时跳过模型渲染，但身上发光时仍然渲染。
     *
     * <p>声波会让幽匿单位发光，而发光轮廓用的是 {@code NO_DEPTH_TEST} 的 outline 通道，会穿墙显示——
     * 玩家正是靠它看见被声波照出来的、还藏在方块里的虫子，所以发光时必须让模型照常进渲染管线
     * （本体在主通道里仍然被方块挡住，只有轮廓露出来）。
     *
     * <p>因此这里不依赖 {@code setInvisible()}，而是根据同步状态直接返回 {@code null}。
     */
    @Nullable
    @Override
    public RenderType getRenderType(SculverfishEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable.isBurrowed() && !animatable.isCurrentlyGlowing()) return null;
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public boolean shouldShowName(SculverfishEntity entity) {
        return !entity.isBurrowed() && super.shouldShowName(entity);
    }
}
