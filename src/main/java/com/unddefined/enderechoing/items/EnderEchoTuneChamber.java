package com.unddefined.enderechoing.items;

import com.unddefined.enderechoing.client.renderer.item.EnderEchoTuneChamberRenderer;
import com.unddefined.enderechoing.api.pearl.EnderEchoPearls;
import com.unddefined.enderechoing.api.anchor.EnderEchoAnchors;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoints;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

import static com.unddefined.enderechoing.server.registry.DataRegistry.*;
import static com.unddefined.enderechoing.api.event.EnderEchoPearlEvent.Cause.*;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_PEARL;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

public class EnderEchoTuneChamber extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public EnderEchoTuneChamber(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // 此操作在创造模式下不生效
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (stack.getCount() != 1) return false;
        if (action != ClickAction.SECONDARY) return false;
        if (!other.is(ENDER_ECHOING_PEARL.asItem())) return false;
        addPearls(player, other);
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (stack.getCount() != 1) return false;
        if (action != ClickAction.SECONDARY) return false;

        ItemStack other = slot.getItem();
        if (!other.is(ENDER_ECHOING_PEARL.asItem())) return false;
        addPearls(player, other);
        slot.setChanged();
        return true;
    }

    private void addPearls(Player player, ItemStack other) {
        var stackPos = other.get(POSITION);
        boolean bound = Boolean.TRUE.equals(other.get(ANCHOR_BOUND));
        boolean result = false;
        if (stackPos != null) {
            var pearlName = other.get(CUSTOM_NAME).getString();
            result = EnderEchoWaypoints.add(player, stackPos.dimension(), stackPos.pos(), pearlName, 0, bound);
        }
        // 分享过来的凭证珍珠：接收方插进调谐腔时一并补登记，否则这个点只有分享者能用
        if (result && bound) {
            var level = player.getServer() == null ? null : player.getServer().getLevel(stackPos.dimension());
            if (level != null && EnderEchoAnchors.isAnchor(level, stackPos.pos())
                    && EnderEchoAnchors.canRegister(player, level, stackPos.pos()))
                EnderEchoAnchors.add(player, level, stackPos.pos());
        }
        EnderEchoPearls.add(player, other.getCount() - (result ? 1 : 0), CONVERT);
        other.shrink(other.getCount());
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<EnderEchoTuneChamber> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) this.renderer = new EnderEchoTuneChamberRenderer();

                return this.renderer;
            }
        });
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
