package com.unddefined.enderechoing.blocks;

import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import com.unddefined.enderechoing.client.gui.TunerMenu;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.server.registry.DataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.WARP_CORE;

public class WarpPlatformBlock extends Block implements EntityBlock {
    public WarpPlatformBlock() {
        super(Properties.of()
                .noOcclusion()
                .sound(SoundType.SCULK_SHRIEKER)
                .explosionResistance(1000.0F)
                .destroyTime(1.5F)
                .pushReaction(PushReaction.DESTROY)
        );
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("menu.title.enderechoing.warpmenu");
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
                return new TunerMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos),true);
            }

            @Override
            public void writeClientSideData(AbstractContainerMenu menu, net.minecraft.network.RegistryFriendlyByteBuf buf) {
                if (menu instanceof TunerMenu t) t.writeClientSideData(buf, new GlobalPos(level.dimension(), pos), true);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, @NotNull Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer P) P.openMenu(state.getMenuProvider(level, pos));
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (entity.isCurrentlyGlowing()) return;
        if (!(level.getBlockEntity(pos) instanceof WarpPlatformBlockEntity BE)) return;
        if (MarkedPositionsManager.getManager(player).teleporters().stream()
                .noneMatch(e -> e.pos().equals(pos) && e.dimension().equals(level.dimension()))) return;
        if (BE.getSelectedPos().equals(GZERO)) return;
        if (player.isShiftKeyDown()) {
            var destination = player.getServer().getLevel(BE.getSelectedPos().dimension());
            if (destination == null) return;
            // 传送前记下出发地，传送成功后起点与终点都可能刷出幽匿螨
            var fromPos = player.position();
            player.changeDimension(new DimensionTransition(destination, BE.getSelectedPos().pos().getCenter(),
                    player.getDeltaMovement(), player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
            SculkBorneBridge.afterTeleport(player, level, fromPos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.getServer() == null) return;
        if (state.is(newState.getBlock())) return;
        level.getServer().getPlayerList().getPlayers().forEach(player -> {
            var M = player.getData(DataRegistry.MARKED_POSITIONS_CACHE.get());
            M.teleporters().removeIf(e -> e.dimension().equals(level.dimension()) && e.pos().equals(pos));
        });
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpPlatformBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(WARP_CORE.get()), new ItemStack(SculkBorneBridge.calibratedShriekerItem()));
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> typeA, BlockEntityType<E> typeB, BlockEntityTicker<? super E> ticker) {
        return typeA == typeB ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, BlockEntityRegistry.WARP_PLATFORM.get(), WarpPlatformBlockEntity::tick);
    }
}
