package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.blocks.EnderEchoCrystalBlock;
import com.unddefined.enderechoing.blocks.EnderEchoTunerBlock;
import com.unddefined.enderechoing.blocks.EnderEchoicResonatorBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("enderechoing");

    public static final DeferredBlock<EnderEchoicResonatorBlock> ENDER_ECHOIC_RESONATOR =
            BLOCKS.register("ender_echoic_resonator", EnderEchoicResonatorBlock::new);
    public static final DeferredBlock<EnderEchoTunerBlock> ENDER_ECHO_TUNER =
            BLOCKS.register("ender_echo_tuner", EnderEchoTunerBlock::new);
    public static final DeferredBlock<EnderEchoCrystalBlock> ENDER_ECHO_CRYSTAL =
            BLOCKS.register("ender_echo_crystal_block", EnderEchoCrystalBlock::new);
}
