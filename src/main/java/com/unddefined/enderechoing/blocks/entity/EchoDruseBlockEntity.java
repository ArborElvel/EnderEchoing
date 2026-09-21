package com.unddefined.enderechoing.blocks.entity;

import com.unddefined.enderechoing.Config;
import com.unddefined.enderechoing.blocks.EchoDruseBlock;
import com.unddefined.enderechoing.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EchoDruseBlockEntity extends BlockEntity {
    private static final int MAX_GROWTH_VALUE = Config.ECHO_DRUSE_MAX_GROWTH_VALUE.get();

    private int growthValue;

    public EchoDruseBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ECHO_DRUSE.get(), pos, state);
        this.growthValue = initialGrowthValue();
    }

    /** 按放置时的生长阶段还原生长值 */
    private int initialGrowthValue() {
        return switch (getBlockState().getValue(EchoDruseBlock.GROWTH_STAGE)) {
            case 2 -> MAX_GROWTH_VALUE / 4; // 25%
            case 3 -> MAX_GROWTH_VALUE / 2; // 50%
            case 4 -> MAX_GROWTH_VALUE * 3 / 4; // 75%
            default -> 0;
        };
    }

    public int getGrowthValue() {
        return growthValue;
    }

    public void setGrowthValue(int growthValue) {
        this.growthValue = Math.max(1, Math.min(growthValue, MAX_GROWTH_VALUE));
        setChanged();
    }
}
