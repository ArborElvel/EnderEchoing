package com.unddefined.enderechoing.compat.sculkborne;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.entities.EnderEchoCrystalEntity;
import com.unddefined.enderechoing.network.packet.OpenEditScreenPacket;
import com.unddefined.enderechoing.server.DataComponents.EnderEchoCrystalSavedData;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.registry.BlockRegistry;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_AMOUNT;
import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_POSITION;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

@EventBusSubscriber(modid = EnderEchoing.MODID)
public final class CompatSculkInteractions {
    @SubscribeEvent
    public static void onRightClickCalibratedShrieker(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!SculkBorneBridge.isCalibratedShrieker(event.getLevel().getBlockState(event.getPos()))) return;

        ItemStack stack = event.getItemStack();
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();

        if (stack.is(ItemRegistry.ENDER_ECHOING_CORE.get())) {
            if (!player.isCreative()) stack.shrink(1);
            level.setBlock(pos, BlockRegistry.ENDER_ECHOIC_RESONATOR.get().defaultBlockState(), 3);
            MarkedPositionsManager.getManager(player).addTeleporter(level, pos);
            if (player.getInventory().hasAnyMatching(item ->
                    item.getItem() == ItemRegistry.ENDER_ECHOING_PEARL.get() && item.get(CUSTOM_NAME) == null)
                    || player.getData(EE_PEARL_AMOUNT.get()) > 0) {
                PacketDistributor.sendToPlayer((ServerPlayer) player, new OpenEditScreenPacket("><", pos));
                player.setData(EE_PEARL_POSITION.get(), pos);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (stack.is(ItemRegistry.ENDER_ECHO_TUNE_CHAMBER.get())) {
            Direction facing = event.getLevel().getBlockState(pos).getValue(findFacing(event.getLevel().getBlockState(pos)));
            if (!player.isCreative()) stack.shrink(1);
            level.setBlock(pos, BlockRegistry.ENDER_ECHO_TUNER.get().defaultBlockState()
                    .setValue(com.unddefined.enderechoing.blocks.EnderEchoTunerBlock.FACING, facing), 3);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (stack.is(ItemRegistry.ENDER_ECHO_CRYSTAL.get())) {
            if (!player.isCreative()) stack.shrink(1);
            level.setBlock(pos, BlockRegistry.ENDER_ECHO_CRYSTAL.get().defaultBlockState(), 3);
            level.addFreshEntity(new EnderEchoCrystalEntity(level, pos));
            EnderEchoCrystalSavedData.get((ServerLevel) level).add(level.dimension(), pos);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static DirectionProperty findFacing(net.minecraft.world.level.block.state.BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("facing") && property instanceof DirectionProperty facing) {
                return facing;
            }
        }
        return com.unddefined.enderechoing.blocks.CalibratedSculkShriekerBlock.FACING;
    }

    private CompatSculkInteractions() {}
}
