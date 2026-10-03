package com.unddefined.enderechoing.items;

import com.unddefined.enderechoing.blocks.entity.EnderEchoCrystalBlockEntity;
import com.unddefined.enderechoing.entities.EnderEchoingEyeEntity;
import com.unddefined.enderechoing.api.crystal.EnderEchoCrystals;
import com.unddefined.enderechoing.server.EnderEchoingEyeLocator;
import com.unddefined.enderechoing.server.registry.DataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.unddefined.enderechoing.Config.*;
import static com.unddefined.enderechoing.blocks.entity.EnderEchoCrystalBlockEntity.zeroUUID;
import static com.unddefined.enderechoing.compat.curios.EnderEchoCuriosPlugin.*;
import static com.unddefined.enderechoing.server.registry.DataRegistry.VISITED_STRUCTURES;

public class EnderEchoingEye extends Item implements ICurioItem {
    // Item instances are singletons shared by every stack and player.
    // Binding state must be scoped per player so one player cannot clear another's binding.
    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    private static class PlayerState {
        EndCrystal crystal;
        EnderEchoCrystalBlockEntity eECrystal;
    }

    public EnderEchoingEye(Properties properties) {
        super(properties.stacksTo(8));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!(level instanceof ServerLevel S)) return InteractionResultHolder.fail(itemStack);

        EnderEchoingEyeLocator.markVisitedIfInside((ServerPlayer) player);
        BlockPos targetPos = EnderEchoingEyeLocator.findNearestUnvisited(S, player.blockPosition(),
                player.getData(VISITED_STRUCTURES.get()).all());
        if (targetPos == null) return InteractionResultHolder.fail(itemStack);

        EnderEchoingEyeEntity eye = new EnderEchoingEyeEntity(level, player.getX(), player.getY(0.5D), player.getZ());
        eye.setItem(itemStack);
        eye.signalTo(targetPos);
        level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
        level.addFreshEntity(eye);

        float pitch = Mth.lerp(level.random.nextFloat(), 0.33F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, pitch);
        itemStack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.swing(hand, true);
        player.getCooldowns().addCooldown(this, 5 * 20);
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public boolean canEquip(SlotContext ctx, ItemStack stack) {
        return CuriosApi.getCuriosInventory(ctx.entity())
                .map(handler -> {
                    for (int i = 0; i < handler.getEquippedCurios().getSlots(); i++) {
                        if (handler.getEquippedCurios().getStackInSlot(i).getItem().equals(Items.ENDER_EYE)) return false;
                    }
                    return ctx.identifier().equals("charm");
                }).orElse(ctx.identifier().equals("charm"));
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        showResonatorName(player);
        if (player.totalExperience < EECrystal_HEAL_XP_COST.get() || player.getHealth() >= player.getMaxHealth()) return;
        PlayerState state = playerStates.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());

        if (state.eECrystal == null || state.eECrystal.getPlayerUUID().equals(zeroUUID)
                || !state.eECrystal.getPlayerUUID().equals(player.getUUID()))
            state.crystal = enderEyeCurioHealTick(player);
        if (state.crystal != null) {
            var UUID = state.crystal.getEntityData().get(DataRegistry.ENDER_EYE_OWNER);
            if (UUID.isPresent() && UUID.get().equals(player.getUUID())) return;
        }

        var level = (ServerLevel) player.level();
        var D = EECrystal_HEAL_DISTANCE.get();
        EnderEchoCrystals.all(level).stream().filter(c -> c.pos().dimension().equals(level.dimension()))
                .min(Comparator.comparingDouble(c -> c.pos().pos().distToCenterSqr(player.getX(), player.getY(), player.getZ())))
                .filter(c -> Math.sqrt(c.pos().pos().distToCenterSqr(player.getX(), player.getY(), player.getZ())) < D)
                .ifPresentOrElse(c -> state.eECrystal = (EnderEchoCrystalBlockEntity) level.getBlockEntity(c.pos().pos()),
                        () -> state.eECrystal = null);
        if (state.eECrystal == null) return;

        if (!state.eECrystal.getPlayerUUID().equals(zeroUUID) && !state.eECrystal.getPlayerUUID().equals(player.getUUID()))
            return;

        state.eECrystal.setPlayerUUID(player.getUUID());

        if (level.getGameTime() % EECrystal_HEAL_INTERVAL.get() * 10 != 0) return;
        player.giveExperiencePoints(-EECrystal_HEAL_XP_COST.get());
        player.heal(EECrystal_HEAL_AMOUNT.get());
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        PlayerState state = playerStates.remove(player.getUUID());
        onEnderEyeUnequip(player);
        if (state != null && state.eECrystal != null) state.eECrystal.setPlayerUUID(zeroUUID);
    }

    @Override
    public boolean isEnderMask(SlotContext slotContext, EnderMan enderMan, ItemStack stack) {
        return true;
    }
}
