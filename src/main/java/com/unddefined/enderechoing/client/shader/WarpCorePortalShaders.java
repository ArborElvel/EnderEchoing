package com.unddefined.enderechoing.client.shader;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.unddefined.enderechoing.EnderEchoing;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/**
 * WarpCore 的 core 骨自绘星云用的 core shader。
 * <p>
 * 光影包不会给物品跑末地传送门那套 shader，而且 Iris 会跳过未接管的原版 shader，
 * 所以这里自己注册一份原版 rendertype_end_portal 的移植版（屏幕空间投影，见 shader 文件），
 * 配合 {@code WarpCorePortalLayer} 在开光影时把世界里的 core 延迟到世界渲染之后，
 * 直接画到主 framebuffer 上。
 */
@EventBusSubscriber(modid = EnderEchoing.MODID, value = Dist.CLIENT)
public final class WarpCorePortalShaders {

    private static ShaderInstance portalShader;
    private static AbstractUniform tintColor;

    private WarpCorePortalShaders() {
    }

    public static ShaderInstance shader() {
        return portalShader;
    }

    public static boolean isReady() {
        return portalShader != null;
    }

    /** 星云染色（默认每帧由 WarpCoreRenderer 用描边色写入）。 */
    public static void setTintColor(float red, float green, float blue) {
        if (tintColor != null) tintColor.set(red, green, blue);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        portalShader = null;
        try {
            event.registerShader(new ShaderInstance(
                    event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "warp_core_portal"),
                    DefaultVertexFormat.NEW_ENTITY), shader -> {
                portalShader = shader;
                tintColor = shader.safeGetUniform("TintColor");
            });
        } catch (Exception e) {
            // 驱动/光影环境编译失败时不要让游戏起不来：保持未就绪，WarpCoreRenderer 会退回原有渲染
            EnderEchoing.LOGGER.error("Failed to register warp_core_portal shader, falling back to end_portal", e);
        }
    }
}
