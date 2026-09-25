package com.unddefined.enderechoing.blocks.entity;

import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;

public class WarpPlatformBlockEntity  extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float animationTime = 0;
    private String selectedName;
    private GlobalPos selectedPos = GZERO;


    public WarpPlatformBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.WARP_PLATFORM.get(), pos, blockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    public static void tick(Level level, BlockPos pos, BlockState state, WarpPlatformBlockEntity blockEntity) {
        // 更新动画时间
        if (level.isClientSide) blockEntity.animationTime += 1;

        //粒子效果
        if (level.isClientSide && level.getRandom().nextFloat() < 0.1) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5 + (level.random.nextDouble() - 0.1) * 0.2,
                    pos.getY() + 1 + (level.random.nextDouble() - 0.1) * 0.2,
                    pos.getZ() + 0.5 + (level.random.nextDouble() - 0.1) * 0.2,
                    (level.random.nextDouble() - 0.5),
                    -level.random.nextDouble(),
                    (level.random.nextDouble() - 0.5));
        }
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

    public String getselectedName() {
        return selectedName;
    }

    public GlobalPos getSelectedPos() {
        return selectedPos;
    }

    public float getAnimationTime() {return animationTime;}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
