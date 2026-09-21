package com.unddefined.enderechoing.blocks;

import com.unddefined.enderechoing.Config;
import com.unddefined.enderechoing.blocks.entity.EchoDruseBlockEntity;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * sculkborne 未加载时的回响晶簇兼容副本：种在幽匿催发体上，靠催发体吸收的死亡经验生长。
 */
public class EchoDruseBlock extends Block implements EntityBlock {
    public static final IntegerProperty GROWTH_STAGE = IntegerProperty.create("growth_stage", 1, 4);

    public EchoDruseBlock() {
        super(Properties.of()
                .lightLevel(state -> 2)
                .sound(SoundType.SCULK)
                .strength(2.2F, 2.2F)
                .requiresCorrectToolForDrops());
        registerDefaultState(defaultBlockState().setValue(GROWTH_STAGE, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROWTH_STAGE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoDruseBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(GROWTH_STAGE)) {
            case 2 -> Block.box(4, 0, 4, 13, 9, 13);
            case 3 -> Block.box(3, 0, 3, 14, 12, 14);
            case 4 -> Block.box(2, 0, 2, 15, 15, 15);
            default -> Block.box(5, 0, 5, 12, 6, 12);
        };
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return switch (state.getValue(GROWTH_STAGE)) {
            case 2 -> List.of(new ItemStack(Items.ECHO_SHARD, 1 + builder.getLevel().getRandom().nextInt(2)));
            case 3 -> List.of(new ItemStack(Items.ECHO_SHARD, 2 + builder.getLevel().getRandom().nextInt(2)));
            case 4 -> List.of(new ItemStack(SculkBorneBridge.echoDruseItem()));
            default -> List.of(new ItemStack(Items.ECHO_SHARD));
        };
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof EchoDruseBlockEntity echoDruse)) return;

        int growthValue = echoDruse.getGrowthValue();
        int growthStage = state.getValue(GROWTH_STAGE);
        int maxGrowthValue = Config.ECHO_DRUSE_MAX_GROWTH_VALUE.get();

        // 增加 growth_value 值，模拟生长过程
        if (growthValue < maxGrowthValue) {
            echoDruse.setGrowthValue(Math.min(growthValue + random.nextInt(2), maxGrowthValue));
        }

        // 根据 growth_value 更新 growth_stage
        int newStage = growthStage;
        if (growthValue > maxGrowthValue * 3 / 4 && growthStage < 4) {
            newStage = 4;
        } else if (growthValue > maxGrowthValue / 2 && growthStage < 3) {
            newStage = 3;
        } else if (growthValue > maxGrowthValue / 4 && growthStage < 2) {
            newStage = 2;
        }

        if (newStage != growthStage) {
            level.setBlock(pos, state.setValue(GROWTH_STAGE, newStage), 3);
        }
    }
}
