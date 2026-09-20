package com.unddefined.enderechoing.items;

import com.unddefined.enderechoing.Config;
import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.client.model.item.EnderEchoingCoreModel;
import com.unddefined.enderechoing.client.renderer.item.EnderEchoingCoreRenderer;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.network.packet.OpenEditScreenPacket;
import com.unddefined.enderechoing.network.packet.RenderEchoNamesPacket;
import com.unddefined.enderechoing.network.packet.SetEchoSoundingPosPacket;
import com.unddefined.enderechoing.network.packet.SetTeleportPosPacket;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import com.unddefined.enderechoing.util.Utils;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.unddefined.enderechoing.Config.EECORE_TP_DISTANCE;
import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.server.registry.BlockRegistry.ENDER_ECHO_CRYSTAL;
import static com.unddefined.enderechoing.server.registry.DataRegistry.*;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;
import static net.minecraft.world.effect.MobEffects.GLOWING;

public class EnderEchoingCore extends Item implements GeoItem {
    private static final String CONTROLLER_NAME = "controller";
    private static final String ANIM_USE = "use";
    private static final RawAnimation USE_ANIM = RawAnimation.begin().thenPlay("ender_echoing_core.use");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    private static class PlayerState {
        int tick;
        int tick2;
        int cost;
        int selectedSlot = -1;
    }

    public EnderEchoingCore(Properties properties) {
        super(properties.stacksTo(2).rarity(Rarity.UNCOMMON));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<EnderEchoingCore> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) this.renderer = new EnderEchoingCoreRenderer(new EnderEchoingCoreModel<>());
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, 0, state -> PlayState.STOP)
                .triggerableAnim(ANIM_USE, USE_ANIM));
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

    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        var manager = MarkedPositionsManager.getManager(player);
        var playerList = Utils.getNearEchoPlayers(level, player);

        if (!player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer S) {
                // 检查是否在冷却中
                if (S.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(itemStack);
                // 检查玩家是否发光，如果发光则无法使用
                if (S.isCurrentlyGlowing()) return InteractionResultHolder.fail(itemStack);
                var sculk_veil = new MobEffectInstance(SculkBorneBridge.veilEffect(), 20 * 3, 0, false, true);
                // 副手持有绑定到其他玩家的珍珠时，优先传送到该玩家的位置
                var offhandPearl = player.getOffhandItem();
                var boundPlayer = offhandPearl.get(ENTITY.get());
                if (boundPlayer != null && !boundPlayer.playerId().equals(player.getUUID())) {
                    ServerPlayer target = S.server.getPlayerList().getPlayer(boundPlayer.playerId());
                    if (target == null) {
                        S.displayClientMessage(Component.translatable("pearl_target_offline"), true);
                        return InteractionResultHolder.fail(itemStack);
                    }
                    // 不可跨维度传送
                    if (!target.level().dimension().equals(level.dimension())) {
                        S.displayClientMessage(Component.translatable("pearl_target_cross_dimension"), true);
                        return InteractionResultHolder.fail(itemStack);
                    }
                    target.addEffect(sculk_veil);
                    // 传送成功后消耗一个绑定该玩家的珍珠（副手中的那一个）
                    offhandPearl.shrink(1);
                    // 渲染传送特效
                    var targetPos = new GlobalPos(level.dimension(), target.blockPosition());
                    PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(player.blockPosition()));
                    PacketDistributor.sendToPlayer(S, new SetTeleportPosPacket(targetPos, true));
                    player.addEffect(sculk_veil);
                    playerList.forEach(e -> {
                        e.addEffect(sculk_veil);
                        PacketDistributor.sendToPlayer(e, new SetEchoSoundingPosPacket(player.blockPosition()));
                    });
                    if (level instanceof ServerLevel SL) triggerAnim(S, GeoItem.getOrAssignId(itemStack, SL), CONTROLLER_NAME, ANIM_USE);

                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(itemStack);
                }
                // 查找最近的EnderEchoicResonator方块
                if (manager.teleporters().stream().filter(e -> e.dimension().equals(level.dimension())).toList().isEmpty())
                    return InteractionResultHolder.fail(itemStack);
                var nearestTeleporterPos = manager.getNearestTeleporter(level, player.blockPosition());
                // 检查玩家是否有空白末影回响珍珠
                int D = EECORE_TP_DISTANCE.get();
                PlayerState state = playerStates.computeIfAbsent(S.getUUID(), ignored -> new PlayerState());
                int cost = (int) Math.round(Math.sqrt(nearestTeleporterPos.pos().distSqr(player.blockPosition())) / D);
                if (cost < 1) cost = 1;
                state.cost = cost;
                if (!player.getInventory().hasAnyMatching(item ->
                        item.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() && item.get(CUSTOM_NAME) == null)
                        && player.getData(EE_PEARL_AMOUNT.get()) < cost)
                    return InteractionResultHolder.fail(itemStack);
                // 渲染传送特效
                PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(player.blockPosition()));
                PacketDistributor.sendToPlayer(S, new SetTeleportPosPacket(nearestTeleporterPos, true));
                player.addEffect(sculk_veil);
                playerList.forEach(e -> {
                    e.addEffect(sculk_veil);
                    PacketDistributor.sendToPlayer(e, new SetEchoSoundingPosPacket(player.blockPosition()));
                });
                if (level instanceof ServerLevel SL) triggerAnim(S, GeoItem.getOrAssignId(itemStack, SL), CONTROLLER_NAME, ANIM_USE);

                player.startUsingItem(hand);
            }
        } else if (player.getData(EE_PEARL_AMOUNT.get()) > 0 || player.getInventory().hasAnyMatching(stack ->
                stack.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() && stack.get(CUSTOM_NAME) == null)) {
            boolean A = level.getBlockEntity(player.blockPosition()) instanceof EnderEchoicResonatorBlockEntity;
            boolean B = player.getData(EE_PEARL_AMOUNT.get()) > 0;
            String name = A ? (B ? ">÷<" : "><") : (B ? "÷" : "");
            if (A) manager.teleporters().stream().filter(e -> e.dimension().equals(level.dimension()))
                    .filter(e -> e.pos().equals(player.blockPosition())).findFirst()
                    .ifPresentOrElse(e -> {
                    }, () -> {
                        manager.teleporters().add(new MarkedPositionsManager.Teleporters(new GlobalPos(level.dimension(), player.blockPosition())));
                        player.displayClientMessage(Component.translatable("new_resonator_added"), true);
                    });

            if (!level.isClientSide()) PacketDistributor.sendToPlayer((ServerPlayer) player, new OpenEditScreenPacket(name, player.blockPosition()));
            player.setData(EE_PEARL_POSITION.get(), player.blockPosition());
        } else player.displayClientMessage(Component.translatable("pearl_not_enough"), true);

        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        super.releaseUsing(stack, level, livingEntity, timeLeft);
        if(level.getRandom().nextInt(5) == 0) livingEntity.addEffect(new MobEffectInstance(GLOWING,300));
        if (level instanceof ServerLevel SL && livingEntity instanceof ServerPlayer S) {
            stopTriggeredAnim(S, GeoItem.getOrAssignId(stack, SL), CONTROLLER_NAME, null);
            PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(BlockPos.ZERO));
            PacketDistributor.sendToPlayer(S, new SetTeleportPosPacket(GZERO, false));
        }
    }

    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        if (level instanceof ServerLevel && livingEntity instanceof ServerPlayer player) {
            PlayerState state = playerStates.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
            int cost = state.cost;
            // 再次检查玩家是否有空白珍珠
            if (!player.getInventory().hasAnyMatching(itemStack ->
                    itemStack.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() && itemStack.get(CUSTOM_NAME) == null)
                    && player.getData(EE_PEARL_AMOUNT.get()) < cost) {
                PacketDistributor.sendToPlayer(player, new SetEchoSoundingPosPacket(BlockPos.ZERO));
                return stack;
            }
            // 消耗空白珍珠
            if (player.getData(EE_PEARL_AMOUNT.get()) > 0) player.setData(EE_PEARL_AMOUNT.get(), player.getData(EE_PEARL_AMOUNT.get()) - cost);
            else player.getInventory().clearOrCountMatchingItems(itemStack ->
                    itemStack.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() &&
                            itemStack.get(CUSTOM_NAME) == null, cost, player.inventoryMenu.getCraftSlots());

            // 设置冷却时间
            player.getCooldowns().addCooldown(this, Config.ENDER_ECHOING_CORE_COOLDOWN.get() * 20);
            state.cost = 0;
        }
        return stack;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
