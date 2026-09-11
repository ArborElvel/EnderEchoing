package com.unddefined.enderechoing.blocks;

import com.unddefined.enderechoing.blocks.entity.EnderEchoCrystalBlockEntity;
import com.unddefined.enderechoing.blocks.entity.EnderEchoTunerBlockEntity;
import com.unddefined.enderechoing.entities.EnderEchoCrystalEntity;
import com.unddefined.enderechoing.network.packet.SendMarkedPositionNamesPacket;
import com.unddefined.enderechoing.network.packet.SendSyncedTeleporterPositionsPacket;
import com.unddefined.enderechoing.network.packet.SetEchoSoundingPosPacket;
import com.unddefined.enderechoing.server.DataComponents.EnderEchoCrystalSavedData;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unddefined.enderechoing.Config.EECrystal_TP_DISTANCE;
import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.server.registry.DataRegistry.EE_PEARL_AMOUNT;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_CORE;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;
import static net.minecraft.world.item.Items.AMETHYST_SHARD;
import static net.minecraft.world.item.Items.ECHO_SHARD;

public class EnderEchoCrystalBlock extends Block implements EntityBlock {
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 15);
    public static final Map<Item, Integer> DYE_CHANNELS = Map.ofEntries(
            Map.entry(Items.RED_DYE, 1), Map.entry(Items.ORANGE_DYE, 2), Map.entry(Items.YELLOW_DYE, 3),
            Map.entry(Items.GREEN_DYE, 4), Map.entry(Items.BLUE_DYE, 5), Map.entry(Items.BLACK_DYE, 6),
            Map.entry(Items.LIME_DYE, 7), Map.entry(Items.LIGHT_BLUE_DYE, 8), Map.entry(Items.PINK_DYE, 9),
            Map.entry(Items.BROWN_DYE, 10), Map.entry(Items.PURPLE_DYE, 11), Map.entry(Items.CYAN_DYE, 12)
    );

    public EnderEchoCrystalBlock() {
        super(Properties.of()
                .noOcclusion()
                .sound(SoundType.SCULK_SHRIEKER)
                .destroyTime(1.5F)
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> 3)
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(CHANNEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHANNEL);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(CHANNEL, 0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderEchoCrystalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(ItemRegistry.ENDER_ECHO_CRYSTAL.get()), new ItemStack(ItemRegistry.CALIBRATED_SCULK_SHRIEKER_ITEM.get()));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, @NotNull Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.FAIL;

        if (stack.getItem().equals(ECHO_SHARD) && stack.get(CUSTOM_NAME) != null) {
            EnderEchoCrystalSavedData.get((ServerLevel) level).getAll().stream().filter(c -> c.pos().dimension().equals(level.dimension())).filter(c-> c.pos().pos().equals(pos)).findFirst().get().setName(stack.get(CUSTOM_NAME).getString());
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.getItem().equals(AMETHYST_SHARD) && state.getValue(CHANNEL) > 0) {
            level.setBlock(pos, state.setValue(CHANNEL, 0), 3);
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        int channel = DYE_CHANNELS.getOrDefault(stack.getItem(), -1);
        if (channel == -1) return ItemInteractionResult.FAIL;
        if (state.getValue(CHANNEL) != channel) {
            level.setBlock(pos, state.setValue(CHANNEL, channel), 3);
            stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (state.is(newState.getBlock())) return;
        EnderEchoCrystalSavedData.get((ServerLevel) level).remove(GlobalPos.of(level.dimension(),pos));
        level.getEntities(new EnderEchoCrystalEntity(level, pos), new AABB(pos), e -> true)
                .forEach(e -> e.remove(Entity.RemovalReason.KILLED));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        var tuner = level.getBlockEntity(pos.above(2));
        var D = EECrystal_TP_DISTANCE.get();
        //定向即时传送
        if (tuner instanceof EnderEchoTunerBlockEntity B && B.getSelectedPos() != null && !B.getSelectedPos().equals(GZERO)
                && player.getData(EE_PEARL_AMOUNT) > 0
                && player.getUseItem().is(ENDER_ECHOING_CORE)
                && B.getSelectedPos().dimension().equals(level.dimension())
                && Math.sqrt(B.getSelectedPos().pos().distSqr(pos)) <= D * D * 0.05){
            var Pos = B.getSelectedPos().pos().getCenter();
            player.teleportTo(Pos.x,Pos.y,Pos.z);
            player.setData(EE_PEARL_AMOUNT, player.getData(EE_PEARL_AMOUNT) - 1);
            return;
        }
        var crystals = EnderEchoCrystalSavedData.get((ServerLevel) level).getAll().stream().filter(C -> C.pos().dimension().equals(level.dimension())).toList();
        if (crystals.size() < 2) return;
        if (entity.isCurrentlyGlowing()) return;
        PacketDistributor.sendToPlayer(player, new SetEchoSoundingPosPacket(pos));
        Map<BlockPos, String> posList = new HashMap<>();
        crystals.stream().filter(p -> Math.sqrt(p.pos().pos().distSqr(pos)) <= D).filter(
                p -> level.getBlockState(p.pos().pos()).getValue(CHANNEL).equals(state.getValue(CHANNEL))
        ).forEach(p -> posList.put(p.pos().pos(),p.name()));
        PacketDistributor.sendToPlayer(player, new SendSyncedTeleporterPositionsPacket(posList.keySet().stream().toList()));
        PacketDistributor.sendToPlayer(player, new SendMarkedPositionNamesPacket(posList));
        if (player.isShiftKeyDown()) posList.keySet().stream().toList().stream()
                .filter(p -> (p.getX() == pos.getX()) && (p.getZ() == pos.getZ()) && (p.getY() < pos.getY()))
                .min(Comparator.comparingInt(BlockPos::getY))
                .ifPresent(p -> player.teleportTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> typeA, BlockEntityType<E> typeB, BlockEntityTicker<? super E> ticker) {
        return typeA == typeB ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, BlockEntityRegistry.ENDER_ECHO_CRYSTAL.get(), EnderEchoCrystalBlockEntity::tick);
    }
}