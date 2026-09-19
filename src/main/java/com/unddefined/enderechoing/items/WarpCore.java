package com.unddefined.enderechoing.items;

import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.client.gui.TunerMenu;
import com.unddefined.enderechoing.client.model.item.WarpCoreModel;
import com.unddefined.enderechoing.client.renderer.item.WarpCoreRenderer;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.network.packet.OpenEditScreenPacket;
import com.unddefined.enderechoing.network.packet.RenderEchoNamesPacket;
import com.unddefined.enderechoing.network.packet.SetEchoSoundingPosPacket;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.unddefined.enderechoing.Config.EECORE_TP_DISTANCE;
import static com.unddefined.enderechoing.server.registry.BlockRegistry.ENDER_ECHO_CRYSTAL;
import static com.unddefined.enderechoing.server.registry.DataRegistry.*;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_PEARL;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;
import static net.minecraft.world.item.Rarity.EPIC;

public class WarpCore extends Item implements GeoItem {
    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    private static class PlayerState {
        int tick;
        int tick2;
        int selectedSlot = -1;
    }

    public WarpCore(Properties properties) {
        super(properties.stacksTo(1).rarity(EPIC));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack, @NotNull LivingEntity livingEntity) {
        return 40;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.CUSTOM;
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof ServerPlayer S)) return;
        PlayerState state = playerStates.computeIfAbsent(S.getUUID(), ignored -> new PlayerState());

        // inventoryTick is called once for every occupied inventory slot.
        // Non-selected stacks must not reset the selected stack's state.
        if (!isSelected) {
            if (S.getMainHandItem().getItem() != this && state.tick > 0) {
                PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(BlockPos.ZERO));
                PacketDistributor.sendToPlayer(S, new RenderEchoNamesPacket(new HashMap<>()));
                state.tick = 0;
                state.selectedSlot = -1;
            }
            return;
        }
        if (state.selectedSlot != slotId) {
            state.selectedSlot = slotId;
            state.tick = 0;
        }

        Map<BlockPos, String> Map = new HashMap<>();
        if (SculkBorneBridge.hasVeil(S) || level.getBlockState(S.blockPosition()).is(ENDER_ECHO_CRYSTAL)) {
            PacketDistributor.sendToPlayer(S, new RenderEchoNamesPacket(Map));
            state.tick2 = 60;
            return;
        }
        if (state.tick2 > 0) {
            state.tick2--;
            return;
        }
        state.tick++;
        PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(S.blockPosition()));
        if (state.tick < 24) return;
        int D = EECORE_TP_DISTANCE.get();
        var manager = MarkedPositionsManager.getManager(S);
        if (manager.teleporters().isEmpty() && manager.markedPositions().isEmpty()) return;
        manager.markedPositions().stream().filter(e -> e.dimension().equals(level.dimension()))
                .filter(e -> Math.sqrt(e.pos().distSqr(S.blockPosition())) < D * 4)
                .forEach(e -> Map.put(e.pos(), e.name()));
        PacketDistributor.sendToPlayer(S, new RenderEchoNamesPacket(Map));
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        if (!(player instanceof ServerPlayer S)) return false;
        if (SculkBorneBridge.hasVeil(S)) return true;
        PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(BlockPos.ZERO));
        PacketDistributor.sendToPlayer(S, new RenderEchoNamesPacket(new HashMap<>()));
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer S) S.openMenu(new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return Component.translatable("menu.title.enderechoing.warpmenu");
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
                    return new TunerMenu(containerId, playerInventory,
                            ContainerLevelAccess.create(level, player.blockPosition()), true);
                }

                @Override
                public void writeClientSideData(AbstractContainerMenu menu, net.minecraft.network.RegistryFriendlyByteBuf buf) {
                    if (menu instanceof TunerMenu t) t.writeClientSideData(buf, new GlobalPos(level.dimension(), player.blockPosition()), true);
                }
            });
        } else if (player.getData(EE_PEARL_AMOUNT) > 0 || player.getInventory().hasAnyMatching(s ->
                s.getItem() == ENDER_ECHOING_PEARL.get() && s.get(CUSTOM_NAME) == null)) {
            boolean A = level.getBlockEntity(player.blockPosition()) instanceof EnderEchoicResonatorBlockEntity;
            boolean B = player.getData(EE_PEARL_AMOUNT) > 0;
            var manager = MarkedPositionsManager.getManager(player);
            String name = A ? (B ? ">÷<" : "><") : (B ? "÷" : "");
            if (A) manager.teleporters().stream().filter(e -> e.dimension().equals(level.dimension()))
                    .filter(e -> e.pos().equals(player.blockPosition())).findFirst()
                    .ifPresent(e -> manager.teleporters().add(new MarkedPositionsManager.Teleporters(new GlobalPos(level.dimension(), player.blockPosition()))));

            if (!level.isClientSide()) PacketDistributor.sendToPlayer((ServerPlayer) player, new OpenEditScreenPacket(name, player.blockPosition()));
            player.setData(EE_PEARL_POSITION.get(), player.blockPosition());
        } else player.displayClientMessage(Component.translatable("pearl_not_enough"),true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
        boolean result = stackPos != null && MarkedPositionsManager.getManager(player)
                .addMarkedPosition(stackPos.dimension(), stackPos.pos(), other.get(CUSTOM_NAME).getString(),
                        0, Boolean.TRUE.equals(other.get(TBOUND)));
        player.setData(EE_PEARL_AMOUNT, player.getData(EE_PEARL_AMOUNT) + other.getCount() - (result ? 1 : 0));
        other.shrink(other.getCount());
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<WarpCore> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) this.renderer = new WarpCoreRenderer(new WarpCoreModel<>());
                return this.renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }
}
