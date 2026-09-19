package com.unddefined.enderechoing.items;

import com.unddefined.enderechoing.blocks.entity.EnderEchoTunerBlockEntity;
import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.network.packet.SetEchoSoundingPosPacket;
import com.unddefined.enderechoing.network.packet.SetTeleportPosPacket;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.unddefined.enderechoing.server.registry.BlockRegistry.ENDER_ECHOIC_RESONATOR;
import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_AMOUNT;
import static com.unddefined.enderechoing.server.registry.DataRegistry.POSITION;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

public class EnderEchoCompass extends Item {
    public EnderEchoCompass(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack, @NotNull LivingEntity livingEntity) {
        return 40;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    @Override
    public InteractionResult useOn(UseOnContext C) {
        if (!C.getLevel().getBlockState(C.getClickedPos()).is(ENDER_ECHOIC_RESONATOR)) return InteractionResult.PASS;
        C.getItemInHand().set(POSITION, GlobalPos.of(C.getLevel().dimension(), C.getClickedPos()));
        MarkedPositionsManager.MarkedPositions name;
        if (C.getPlayer() == null) return InteractionResult.PASS;
        name = MarkedPositionsManager.getManager(C.getPlayer()).markedPositions().stream().filter(t -> t.pos().equals(C.getClickedPos())).findFirst().orElse(null);
        if (name == null) return InteractionResult.PASS;
        C.getItemInHand().set(CUSTOM_NAME, Component.literal(name.name()));
        C.getPlayer().swing(C.getHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && player instanceof ServerPlayer S) {
            if (player.isCurrentlyGlowing()) return InteractionResultHolder.fail(stack);
            if (!(player.level().getBlockEntity(player.blockPosition()) instanceof EnderEchoicResonatorBlockEntity))
                return InteractionResultHolder.fail(stack);
            var pos = player.getLastDeathLocation().orElse(null);
            if (stack.get(POSITION) != null) {
                MarkedPositionsManager.getManager(S).teleporters().stream().filter(t -> t.globalPos().equals(stack.get(POSITION))).findFirst().ifPresentOrElse(t -> {}, () -> {
                    stack.remove(POSITION);
                    stack.remove(CUSTOM_NAME);
                });
            }
            pos = stack.get(POSITION) == null ? pos : stack.get(POSITION);
            if (pos == null) return InteractionResultHolder.fail(stack);

            if (!player.getInventory().hasAnyMatching(item ->
                    item.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() && item.get(CUSTOM_NAME) == null)
                    && player.getData(EE_PEARL_AMOUNT.get()) < 1)
                return InteractionResultHolder.fail(stack);

            if (!level.dimension().equals(pos.dimension()) && !(level.getBlockEntity(player.blockPosition().above(2)) instanceof EnderEchoTunerBlockEntity E && E.checkMultiblock()))
                return InteractionResultHolder.fail(stack);

            // 渲染传送特效
            PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(player.blockPosition()));
            PacketDistributor.sendToPlayer(S, new SetTeleportPosPacket(pos, true));

            player.addEffect(new MobEffectInstance(SculkBorneBridge.veilEffect(), 20 * 3, 0, false, true));
            player.startUsingItem(hand);
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown() && stack.get(POSITION) != null) {
            stack.remove(POSITION);
            stack.remove(CUSTOM_NAME);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        // 当玩家释放使用物品时，移除动画层
        super.releaseUsing(stack, level, livingEntity, timeLeft);

        if (level instanceof ServerLevel SL && livingEntity instanceof ServerPlayer S) {
            S.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400));
            PacketDistributor.sendToPlayer(S, new SetEchoSoundingPosPacket(BlockPos.ZERO));
            PacketDistributor.sendToPlayer(S, new SetTeleportPosPacket(new GlobalPos(level.dimension(), BlockPos.ZERO), false));
        }
    }

    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        if (level instanceof ServerLevel && livingEntity instanceof ServerPlayer player) {
            var pos = player.getLastDeathLocation().orElse(null);
            if (pos == null) return stack;

            if (!level.dimension().equals(pos.dimension()) || !(level.getBlockEntity(player.blockPosition().above(2)) instanceof EnderEchoTunerBlockEntity E && E.checkMultiblock()))
                return stack;

            // 消耗一个没有保存数据的珍珠
            if (player.getData(EE_PEARL_AMOUNT.get()) > 0)
                player.setData(EE_PEARL_AMOUNT.get(), player.getData(EE_PEARL_AMOUNT.get()) - 1);
            else player.getInventory().clearOrCountMatchingItems(itemStack ->
                    itemStack.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() &&
                            itemStack.get(CUSTOM_NAME) == null, 1, player.inventoryMenu.getCraftSlots());
            return stack;
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        var P = stack.get(POSITION.get());
        if (P != null)
            tooltip.add(Component.translatable("item.enderechoing.ender_echoing_pearl.position", P.pos().toShortString(),
                    Component.translationArg(P.dimension().location())));
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
    }

}
