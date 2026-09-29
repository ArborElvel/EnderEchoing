package com.unddefined.enderechoing.blocks.entity;

import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

import static com.unddefined.enderechoing.Config.EECrystal_HEAL_DISTANCE;

public class EnderEchoCrystalBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final UUID zeroUUID = new UUID(0, 0);
    private UUID playerUUID = zeroUUID;

    public EnderEchoCrystalBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.ENDER_ECHO_CRYSTAL.get(), pos, blockState);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID == null ? zeroUUID : playerUUID;
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnderEchoCrystalBlockEntity self) {
        if (level == null || level.isClientSide) return;

        // 每秒让 sculkborne 判定一次：徘徊者有没有极小概率出现在仪器上
        if (level.getGameTime() % 20 == 0) SculkBorneBridge.deviceTick(level, pos);

        if (self.playerUUID == null || self.playerUUID.equals(zeroUUID)) return;
        var player = level.getPlayerByUUID(self.playerUUID);
        var D = EECrystal_HEAL_DISTANCE.get();
        if (player == null || player.distanceToSqr(pos.getCenter()) > D * D || player.getHealth() >= player.getMaxHealth())
            self.setPlayerUUID(zeroUUID);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("player")) setPlayerUUID(tag.getUUID("player"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("player", playerUUID == null ? zeroUUID : playerUUID);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenPlay("item")))
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
