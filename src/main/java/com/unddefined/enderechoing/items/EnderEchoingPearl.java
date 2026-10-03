package com.unddefined.enderechoing.items;

import com.unddefined.enderechoing.network.packet.OpenEditScreenPacket;
import com.unddefined.enderechoing.server.DataComponents.EntityData;
import com.unddefined.enderechoing.api.pearl.EnderEchoPearls;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import com.unddefined.enderechoing.api.anchor.EnderEchoAnchors;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static com.unddefined.enderechoing.server.registry.DataRegistry.*;
import static com.unddefined.enderechoing.api.event.EnderEchoPearlEvent.Cause.*;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

public class EnderEchoingPearl extends Item {
    public EnderEchoingPearl(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static void handleSetDataRequest(ServerPlayer player, String name, int iconIndex, ItemStack handStack, Level level) {
        var Name = name.isEmpty() ? Component.translatable("item.enderechoing.ender_echoing_pearl").getString() : name;
        var playerPos = player.blockPosition();
        var pearl = new ItemStack(ItemRegistry.ENDER_ECHOING_PEARL.get());
        var targetPosition = player.getData(EE_PEARL_POSITION.get());
        boolean bound = EnderEchoAnchors.isAnchor(level, targetPosition);
        pearl.set(CUSTOM_NAME, null);

        if (handStack.getItem() instanceof EnderEchoingPearl) {
            //pearl.use()标记
            player.setExperiencePoints(player.totalExperience - 80);
            handStack.remove(ENTITY.get());
            handStack.set(DataComponents.CUSTOM_NAME, Component.literal(Name));
            handStack.set(POSITION.get(), new GlobalPos(level.dimension(), playerPos));
            handStack.set(ANCHOR_BOUND.get(), bound);
        } else {
            //非pearl.use()标记
            if (player.getData(EE_PEARL_AMOUNT.get()) > 0) {
                int icon = iconIndex != -1 ? iconIndex : 0;
                // 路径点 Pre 被拦截时不扣经验、不扣珍珠
                if (!EnderEchoWaypoints.add(player, level.dimension(), targetPosition, name, icon, bound)) return;
                EnderEchoPearls.add(player, -1, WAYPOINT);
                player.setExperiencePoints(player.totalExperience - 80);
            } else {
                player.setExperiencePoints(player.totalExperience - 80);
                var pearlStack = player.getInventory().getItem(player.getInventory().findSlotMatchingItem(pearl));
                var CopyStack = pearlStack.copyWithCount(1);
                CopyStack.set(DataComponents.CUSTOM_NAME, Component.literal(Name));
                CopyStack.set(POSITION.get(), new GlobalPos(level.dimension(), targetPosition));
                CopyStack.set(ANCHOR_BOUND.get(), bound);
                player.getInventory().add(CopyStack);
                pearlStack.shrink(1);
            }
        }
    }

    /**
     * 是否持有指向该点的分享凭证：一颗在该点造出的 ANCHOR_BOUND 珍珠。
     */
    public static boolean hasAnchorToken(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        return player.getInventory().hasAnyMatching(stack -> {
            if (!stack.is(ItemRegistry.ENDER_ECHOING_PEARL.get())) return false;
            if (!Boolean.TRUE.equals(stack.get(ANCHOR_BOUND.get()))) return false;
            var P = stack.get(POSITION.get());
            return P != null && P.dimension().equals(dimension) && P.pos().equals(pos);
        });
    }

    /**
     * 站在未登记的锚点上时，若手里持有指向该点的 ANCHOR_BOUND 珍珠，就替玩家补上登记。
     * 登记原本只落在放置者名下，分享出去的珍珠让接收方也能拿到同一个锚点。
     *
     * @return true 表示已完成补登记，调用方不应再拒绝这次操作
     */
    public static boolean tryRebindAnchor(Player player, Level level, BlockPos pos) {
        if (!EnderEchoAnchors.isAnchor(level, pos)) return false;
        if (!hasAnchorToken(player, level.dimension(), pos)) return false;
        if (!EnderEchoAnchors.canRegister(player, level, pos)) return false;
        EnderEchoAnchors.add(player, level, pos);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        var positionData = itemStack.get(POSITION.get());
        var entityData = itemStack.get(ENTITY.get());
        if (level.isClientSide) return InteractionResultHolder.fail(itemStack);

        if (player.isShiftKeyDown() && (positionData != null || entityData != null)) {
            itemStack.remove(POSITION.get());
            itemStack.remove(ENTITY.get());
            itemStack.remove(DataComponents.CUSTOM_NAME);
            return InteractionResultHolder.success(itemStack);
        }

        if (positionData == null && entityData == null){
            var pos = player.blockPosition();
            // 未登记的锚点：手里持有指向该点的 ANCHOR_BOUND 珍珠时允许补登记，否则拒绝
            if (EnderEchoAnchors.isUnregisteredAnchor(player, level, pos) && !tryRebindAnchor(player, level, pos)){
                player.displayClientMessage(Component.translatable("item.enderechoing.ender_echoing_core.reject"), true);
                return InteractionResultHolder.fail(itemStack);}
            PacketDistributor.sendToPlayer((ServerPlayer) player, new OpenEditScreenPacket(
                EnderEchoAnchors.isAnchor(level, pos) ? "><" : "", BlockPos.ZERO));}

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ServerPlayer boundPlayer)) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            // 绑定玩家：POSITION 只存 GlobalPos，玩家 ID 存入 ENTITY(EntityData)。
            // 创造模式下 vanilla 传入的是副本，必须改写玩家实际手持的物品。
            ItemStack heldStack = player.getItemInHand(hand);
            if (!(heldStack.getItem() instanceof EnderEchoingPearl)) return InteractionResult.PASS;
            heldStack.set(ENTITY.get(), new EntityData(boundPlayer.getUUID()));
            heldStack.set(DataComponents.CUSTOM_NAME, Component.literal(boundPlayer.getGameProfile().getName()));
            heldStack.remove(POSITION.get());
            heldStack.remove(ANCHOR_BOUND.get());
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
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
