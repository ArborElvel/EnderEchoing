package com.unddefined.enderechoing.compat.sculkborne;

import com.mojang.serialization.Codec;
import com.unddefined.enderechoing.blocks.CalibratedSculkShriekerBlock;
import com.unddefined.enderechoing.blocks.EchoDruseBlock;
import com.unddefined.enderechoing.effects.SculkVeilEffect;
import com.unddefined.enderechoing.items.EchoDruse;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;

import java.util.function.Supplier;

import static com.unddefined.enderechoing.EnderEchoing.MODID;

public final class CompatSculkRegistry {
    public static final boolean ACTIVE = !SculkBorneBridge.isLoaded();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final DeferredBlock<CalibratedSculkShriekerBlock> CALIBRATED_SCULK_SHRIEKER =
            BLOCKS.register("calibrated_sculk_shrieker", CalibratedSculkShriekerBlock::new);
    public static final DeferredItem<BlockItem> CALIBRATED_SCULK_SHRIEKER_ITEM =
            ITEMS.registerSimpleBlockItem("calibrated_sculk_shrieker", CALIBRATED_SCULK_SHRIEKER);
    public static final DeferredItem<EchoDruse> ECHO_DRUSE = ITEMS.registerItem("echo_druse", EchoDruse::new);
    public static final DeferredBlock<EchoDruseBlock> ECHO_DRUSE_BLOCK =
            BLOCKS.register("echo_druse_block", EchoDruseBlock::new);
    public static final DeferredItem<BlockItem> ECHO_DRUSE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("echo_druse_block", ECHO_DRUSE_BLOCK);
    public static final DeferredItem<Item> RHYME_SHARD = ITEMS.registerSimpleItem("rhyme_shard", new Item.Properties()
            .rarity(Rarity.UNCOMMON).component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredHolder<MobEffect, SculkVeilEffect> SCULK_VEIL =
            MOB_EFFECTS.register("sculk_veil", SculkVeilEffect::new);
    /** sculkborne 未加载时的发光药水副本：基础 3 分钟，发光时长可抵消影匿的黑暗积累 */
    public static final DeferredHolder<Potion, Potion> GLOWING =
            POTIONS.register("glowing", () -> new Potion(
                    new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 3)
            ));

    public static final Supplier<AttachmentType<Long>> SCULK_VEIL_START = ATTACHMENTS.register(
            "sculk_veil_start", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> SCULK_VEIL_LAST_TICK = ATTACHMENTS.register(
            "sculk_veil_last_tick", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> SCULK_VEIL_TOTAL = ATTACHMENTS.register(
            "sculk_veil_total", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> GLOWING_START = ATTACHMENTS.register(
            "glowing_start", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> GLOWING_LAST_TICK = ATTACHMENTS.register(
            "glowing_last_tick", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build()
    );
    public static final Supplier<AttachmentType<Long>> GLOWING_TOTAL = ATTACHMENTS.register(
            "glowing_total", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build()
    );

    private CompatSculkRegistry() {}
}
