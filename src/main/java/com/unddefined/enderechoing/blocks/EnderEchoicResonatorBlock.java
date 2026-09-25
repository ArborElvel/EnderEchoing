package com.unddefined.enderechoing.blocks;

import com.unddefined.enderechoing.blocks.entity.EnderEchoTunerBlockEntity;
import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.network.packet.SendMarkedPositionNamesPacket;
import com.unddefined.enderechoing.network.packet.SendSyncedTeleporterPositionsPacket;
import com.unddefined.enderechoing.network.packet.SetEchoSoundingPosPacket;
import com.unddefined.enderechoing.network.packet.SetTeleportPosPacket;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.server.registry.DataRegistry;
import com.unddefined.enderechoing.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.blocks.EnderEchoTunerBlock.CHARGED;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_CORE;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_PEARL;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

public class EnderEchoicResonatorBlock extends Block implements EntityBlock {
    public static final BooleanProperty CoolDown = BooleanProperty.create("cooldown");

    public EnderEchoicResonatorBlock() {
        super(Properties.of()
                .noOcclusion()
                .sound(SoundType.SCULK_SHRIEKER)
                .explosionResistance(1000.0F)
                .destroyTime(1.5F)
                .pushReaction(PushReaction.DESTROY)
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(CoolDown, true).setValue(CHARGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CoolDown, CHARGED);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> typeA, BlockEntityType<E> typeB, BlockEntityTicker<? super E> ticker) {
        return typeA == typeB ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderEchoicResonatorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(ENDER_ECHOING_CORE.get()), new ItemStack(SculkBorneBridge.calibratedShriekerItem()));
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, @NotNull Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.FAIL;
        if (stack.is(Items.DRAGON_BREATH) && !state.getValue(CHARGED)) {
            level.setBlock(pos, state.setValue(CHARGED, true), 3);
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.FAIL;
    }

    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, state.setValue(CoolDown, true), 3);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (entity.isCurrentlyGlowing()) return;
        var manager = MarkedPositionsManager.getManager(player);
        if (manager.teleporters().isEmpty() && manager.markedPositions().isEmpty()) return;
        if (manager.teleporters().stream()
                .noneMatch(e -> e.pos().equals(pos) && e.dimension().equals(level.dimension()))) return;
        level.scheduleTick(pos, this, 40);
        if (!state.getValue(CoolDown)) return;
        var playerList = Utils.getNearEchoPlayers(level, player);
        playerList.forEach(e -> {
            PacketDistributor.sendToPlayer(e, new SetEchoSoundingPosPacket(pos));
            e.addEffect(new MobEffectInstance(SculkBorneBridge.veilEffect(), state.getValue(CHARGED) ? 300 : 60));
        });
        PacketDistributor.sendToPlayer(player, new SetEchoSoundingPosPacket(pos));
        player.addEffect(new MobEffectInstance(SculkBorneBridge.veilEffect(), state.getValue(CHARGED) ? 300 : 60));
        level.setBlock(pos, state.setValue(CoolDown, false).setValue(CHARGED, false), 3);
        //获取目的地名称
        var posList = manager.getTeleporterPositions(level);
        var map = manager.getMarkedTeleportersMap(level);
        var pearlList = player.getInventory().items.stream().filter(i -> i.is(ENDER_ECHOING_PEARL.get())).toList();
        pearlList.forEach(itemStack -> {
            var p = itemStack.get(DataRegistry.POSITION);
            var n = itemStack.get(CUSTOM_NAME);
            if (p != null && n != null && p.dimension().equals(level.dimension()) && posList.contains(p.pos()))
                map.put(p.pos(), n.getString());
        });
        if (!map.isEmpty()) PacketDistributor.sendToPlayer(player, new SendMarkedPositionNamesPacket(map));

        GlobalPos targetPos = null;
        var tuner = level.getBlockEntity(pos.above(2));
        //获取定向目的地
        if (tuner instanceof EnderEchoTunerBlockEntity B)
            if (!B.getSelectedPos().equals(GZERO) && (B.getSelectedPos().dimension().equals(level.dimension()) || B.checkMultiblock()))
                targetPos = B.getSelectedPos();
        // 传送
        if (targetPos != null) PacketDistributor.sendToPlayer(player, new SetTeleportPosPacket(targetPos, true));
        else PacketDistributor.sendToPlayer(player, new SendSyncedTeleporterPositionsPacket(posList));
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, BlockEntityRegistry.ENDER_ECHOIC_RESONATOR.get(), EnderEchoicResonatorBlockEntity::tick);
    }
}
