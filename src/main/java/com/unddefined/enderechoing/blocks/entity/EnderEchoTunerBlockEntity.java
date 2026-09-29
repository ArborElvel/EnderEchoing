package com.unddefined.enderechoing.blocks.entity;

import com.mojang.logging.LogUtils;
import com.unddefined.enderechoing.blocks.EnderEchoTunerBlock;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.server.registry.BlockRegistry.*;
import static net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR;
import static net.minecraft.world.level.block.Blocks.SCULK_SENSOR;

public class EnderEchoTunerBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String selectedName;
    private GlobalPos selectedPos = GZERO;

    private BlockPos anchorPos;
    private int Acharge = 0;

    public EnderEchoTunerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.ENDER_ECHO_TUNER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnderEchoTunerBlockEntity blockEntity) {
        float offset = state.getValue(EnderEchoTunerBlock.FACING) == Direction.DOWN ? 0.7f : 0;

        // 每秒让 sculkborne 判定一次：徘徊者有没有极小概率出现在仪器上
        if (!level.isClientSide && level.getGameTime() % 20 == 0) SculkBorneBridge.deviceTick(level, pos);

        //粒子效果
        if (level.isClientSide && level.getRandom().nextFloat() < 0.15) {
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + Math.clamp(level.random.nextDouble(), 0.2, 0.8),
                    pos.getY() + 1.1 - offset,
                    pos.getZ() + Math.clamp(level.random.nextDouble(), 0.2, 0.8),
                    0, 0, 0);
        }
    }

    public boolean checkMultiblock() {
        if (level == null) return false;
        Set<BlockPos> visited = new HashSet<>();
        int[] A = {0, 0, 0, 0};
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++) {
                    BlockPos target = worldPosition.offset(x, y - 2, z);
                    BlockState S = level.getBlockState(target);
                    if (S.is(ENDER_ECHO_CRYSTAL)) Add(A, 0, visited, target);
                    else if (S.is(ENDER_ECHOIC_RESONATOR)) Add(A, 1, visited, target);
                    else if (S.is(RESPAWN_ANCHOR)) Add(A, 2, visited, target);
                    else if (S.is(SCULK_SENSOR)) Add(A, 3, visited, target);
                }
        return A[0] >= 4 && A[1] >= 1 && A[2] >= 1 && A[3] >= 8 && Acharge > 1;
    }

    public void Add(int[] A, int i, Set<BlockPos> visited, BlockPos target) {
        A[i]++;
        visited.add(target);
        if (i == 2) {
            anchorPos = target;
            if (level == null) return;
            Acharge = level.getBlockState(target).getValue(RespawnAnchorBlock.CHARGE);
        }
    }
    public boolean consumeAnchorCharge(){
        if(level == null) return false;
        if(anchorPos == null) return false;
        BlockState state = level.getBlockState(anchorPos);
        if(!state.is(RESPAWN_ANCHOR)) return false;
        int charge = state.getValue(RespawnAnchorBlock.CHARGE);
        if(charge <= 0) return false;
        level.setBlock(anchorPos, state.setValue(RespawnAnchorBlock.CHARGE, charge - 1), 3);
        return true;
    }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        BlockPos pos = GZERO.pos();
        ResourceKey<Level> Dimension = GZERO.dimension();
        if (tag.contains("pos")) pos = BlockPos.of(tag.getLong("pos"));
        if (tag.contains("dimension"))
            Dimension = ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.parse("dimension")),
                    ResourceLocation.parse(tag.getString("dimension")));
        selectedPos = new GlobalPos(Dimension, pos);
        if (tag.contains("name")) selectedName = tag.getString("name");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (selectedPos == null || selectedName == null) return;
        tag.putLong("pos", selectedPos.pos().asLong());
        tag.putString("dimension", selectedPos.dimension().location().toString());
        tag.putString("name", selectedName);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void setSelectedPosition(GlobalPos P, String n) {
        this.selectedName = P.equals(GZERO) ? "" : n;
        this.selectedPos = P;
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        setChanged();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 20, event -> event.setAndContinue(RawAnimation.begin().thenLoop("tuner"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public String getselectedName() {
        return selectedName;
    }

    public GlobalPos getSelectedPos() {
        return selectedPos;
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }
}
