package com.unddefined.enderechoing.client;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.gui.screen.TunerScreen;
import com.unddefined.enderechoing.client.renderer.block.*;
import com.unddefined.enderechoing.client.renderer.entity.EnderEchoCrystalEntityRenderer;
import com.unddefined.enderechoing.compat.sculkborne.CompatSculkRegistry;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.server.registry.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;

import static com.unddefined.enderechoing.EnderEchoing.TUNER_MENU;
import static com.unddefined.enderechoing.server.registry.DataRegistry.POSITION;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHO_COMPASS;

@Mod(value = EnderEchoing.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = EnderEchoing.MODID, value = Dist.CLIENT)
public class EnderEchoingClient {
    private static final Minecraft mc = Minecraft.getInstance();
    public static PostChain sculkVeilPostChain = null;
    public static PostChain deepDarkVeilPostChain = null;

    public EnderEchoingClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (CompatSculkRegistry.ACTIVE) {
                try {
                    ResourceLocation veil = ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "shaders/post/sculk_veil.json");
                    sculkVeilPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), veil);
                    deepDarkVeilPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), veil);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            ItemProperties.register(ENDER_ECHO_COMPASS.get(),
                    ResourceLocation.fromNamespaceAndPath(EnderEchoing.MODID, "angle"),
                    new CompassItemPropertyFunction((level, stack, entity) -> {
                        if (entity instanceof Player player) return stack.get(POSITION) == null ?
                                player.getLastDeathLocation().orElse(null) : stack.get(POSITION);
                        return null;
                    }));

            BlockEntityRenderers.register(BlockEntityRegistry.ENDER_ECHOIC_RESONATOR.get(),
                    context -> new EnderEchoicResonatorRenderer());
            BlockEntityRenderers.register(BlockEntityRegistry.ENDER_ECHO_TUNER.get(),
                    context -> new EnderEchoTunerRenderer());
            BlockEntityRenderers.register(BlockEntityRegistry.WARP_PLATFORM.get(),
                    context -> new WarpPlatformRenderer());
            BlockEntityRenderers.register(BlockEntityRegistry.ENDER_ECHO_CRYSTAL.get(),
                    context -> new EnderEchoCrystalBlockRenderer());
            if (CompatSculkRegistry.ACTIVE) {
                BlockEntityRenderers.register(BlockEntityRegistry.CALIBRATED_SCULK_SHRIEKER.get(),
                        context -> new CalibratedSculkShriekerRenderer());
            }
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.ENDER_ECHO_CRYSTAL_ENTITY.get(), EnderEchoCrystalEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ENDER_ECHOING_EYE_ENTITY.get(),
                c -> new ThrownItemRenderer<>(c, 1.0F, true));
    }

    @SubscribeEvent
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TUNER_MENU.get(), TunerScreen::new);
    }
}
