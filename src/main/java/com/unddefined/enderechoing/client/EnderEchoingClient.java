package com.unddefined.enderechoing.client;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.client.gui.screen.TunerScreen;
import com.unddefined.enderechoing.client.particles.ParticleDirectlyMovingDust;
import com.unddefined.enderechoing.client.renderer.block.*;
import com.unddefined.enderechoing.client.renderer.entity.EnderEchoCrystalEntityRenderer;
import com.unddefined.enderechoing.client.renderer.entity.SculkSpreaderEntityRenderer;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.server.registry.EntityRegistry;
import com.unddefined.enderechoing.server.registry.ParticlesRegistry;
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
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;

import static com.unddefined.enderechoing.EnderEchoing.TUNER_MENU;
import static com.unddefined.enderechoing.server.registry.DataRegistry.POSITION;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHO_COMPASS;


// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = EnderEchoing.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = EnderEchoing.MODID, value = Dist.CLIENT)
public class EnderEchoingClient {
    private static final Minecraft mc = Minecraft.getInstance();
    public static PostChain sculkVeilPostChain = null;
    public static PostChain deepDarkVeilPostChain = null;
    public static PostChain sculkIntrusionPostChain = null;

    public EnderEchoingClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                sculkVeilPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        ResourceLocation.fromNamespaceAndPath("enderechoing", "shaders/post/sculk_veil.json"));
                deepDarkVeilPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        ResourceLocation.fromNamespaceAndPath("enderechoing", "shaders/post/sculk_veil.json"));
                sculkIntrusionPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        ResourceLocation.fromNamespaceAndPath("enderechoing", "shaders/post/sculk_intrusion.json"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ItemProperties.register(ENDER_ECHO_COMPASS.get(),
                    ResourceLocation.fromNamespaceAndPath("enderechoing", "angle"),
                    new CompassItemPropertyFunction((level, stack, entity) -> {
                        if (entity instanceof Player player) return stack.get(POSITION) == null ?
                                    player.getLastDeathLocation().orElse(null) : stack.get(POSITION);
                        return null;
                    })
            );
        });

        // Register block entity renderers
        event.enqueueWork(() -> BlockEntityRenderers.register(BlockEntityRegistry.ENDER_ECHOIC_RESONATOR.get(),
                context -> new EnderEchoicResonatorRenderer()));
        event.enqueueWork(() -> BlockEntityRenderers.register(BlockEntityRegistry.CALIBRATED_SCULK_SHRIEKER.get(),
                context -> new CalibratedSculkShriekerRenderer()));
        event.enqueueWork(() -> BlockEntityRenderers.register(BlockEntityRegistry.SCULK_WHISPER.get(),
                context -> new SculkWhisperRenderer()));
        event.enqueueWork(() -> BlockEntityRenderers.register(BlockEntityRegistry.ENDER_ECHO_TUNER.get(),
                context -> new EnderEchoTunerRenderer()));
        event.enqueueWork(() -> BlockEntityRenderers.register(BlockEntityRegistry.ENDER_ECHO_CRYSTAL.get(),
                context -> new EnderEchoCrystalBlockRenderer()));
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.ENDER_ECHO_CRYSTAL_ENTITY.get(), EnderEchoCrystalEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SCULK_SPREADER_ENTITY.get(), SculkSpreaderEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ENDER_ECHOING_EYE_ENTITY.get(),
                c -> new ThrownItemRenderer<>(c, 1.0F, true));
    }

    @SubscribeEvent
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TUNER_MENU.get(), TunerScreen::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticlesRegistry.DIRECT_MOVING_DUST.get(), ParticleDirectlyMovingDust.Provider::new);
    }

//    @SubscribeEvent
//    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
//        // 为玩家渲染器添加影匿渲染层（有bug）
//        event.getSkins().forEach((skin) -> {
//            EntityRenderer<? extends Player> playerRenderer = event.getSkin(skin);
//            if (playerRenderer instanceof PlayerRenderer renderer) {
//                renderer.addLayer(new SculkVeilLayer(renderer));
//            }
//        });
//    }
}
