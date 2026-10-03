package com.unddefined.enderechoing.compat.curios;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.network.packet.RenderEchoNamesPacket;
import com.unddefined.enderechoing.api.crystal.EnderEchoCrystals;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoints;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.unddefined.enderechoing.Config.*;
import static com.unddefined.enderechoing.server.registry.DataRegistry.ENDER_EYE_OWNER;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_EYE;

@EventBusSubscriber(modid = EnderEchoing.MODID)
public class EnderEchoCuriosPlugin {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent evt) {
        evt.registerItem(CuriosCapability.ITEM,
                (stack, context) -> new ICurio() {
                    @Override
                    public ItemStack getStack() {
                        return stack;
                    }

                    @Override
                    public boolean canEquip(SlotContext ctx) {
                        return CuriosApi.getCuriosInventory(ctx.entity())
                                .map(handler -> {
                                    for (int i = 0; i < handler.getEquippedCurios().getSlots(); i++) {
                                        if (handler.getEquippedCurios().getStackInSlot(i).getItem().equals(ENDER_ECHOING_EYE.get())) return false;
                                    }
                                    return ctx.identifier().equals("charm");
                                }).orElse(ctx.identifier().equals("charm"));

                    }

                    @Override
                    public void curioTick(SlotContext slotContext) {
                        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
                        showResonatorName(player);
                        if (player.totalExperience < EndCrystal_HEAL_XP_COST.getAsInt()
                                || player.getHealth() >= player.getMaxHealth()) return;
                        enderEyeCurioHealTick(player);
                    }

                    @Override
                    public void onUnequip(SlotContext slotContext, ItemStack newStack) {
                        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
                        onEnderEyeUnequip(player);
                    }
                }, Items.ENDER_EYE);
    }

    public static EndCrystal enderEyeCurioHealTick(ServerPlayer player) {
        var level = (ServerLevel) player.level();
        if (level.getDragonFight() != null) return null;

        EndCrystal crystal = level.getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(EECrystal_HEAL_DISTANCE.get()))
                .stream().min(Comparator.comparingDouble(c -> c.distanceToSqr(player))).orElse(null);
        if (crystal == null) return null;
        var tag = crystal.getEntityData().get(ENDER_EYE_OWNER);
        if (tag.isPresent() && !tag.get().equals(player.getUUID())) return null;

        crystal.getEntityData().set(ENDER_EYE_OWNER, Optional.of(player.getUUID()));

        if (level.getGameTime() % EndCrystal_HEAL_INTERVAL.get() * 10 != 0) return crystal;
        player.giveExperiencePoints(-EndCrystal_HEAL_XP_COST.get());
        player.heal(EndCrystal_HEAL_AMOUNT.get());
        return crystal;
    }

    public static void showResonatorName(ServerPlayer player) {
        Map<BlockPos, String> posName = new HashMap<>();
        var waypoints = EnderEchoWaypoints.of(player);
        var crystals = EnderEchoCrystals.all((ServerLevel) player.level()).stream()
                        .filter(c -> c.pos().dimension().equals(player.level().dimension())).toList();

        waypoints.stream().filter(e -> e.dimension().equals(player.level().dimension()))
                .filter(e -> e.pos().distToCenterSqr(player.position()) < 25)
                .forEach(e -> posName.put(e.pos(), e.name()));

        crystals.stream().filter(e -> e.pos().pos().distToCenterSqr(player.position()) < 25)
                .forEach(e -> posName.put(e.pos().pos(), e.name()));

        PacketDistributor.sendToPlayer(player, new RenderEchoNamesPacket(posName));
    }

    public static void onEnderEyeUnequip(ServerPlayer player) {
        Map<BlockPos, String> posName = new HashMap<>();
        PacketDistributor.sendToPlayer(player, new RenderEchoNamesPacket(posName));
        // Capability wrappers are recreated on every query, so no remembered EndCrystal
        // reference survives to this callback. Clear by authoritative owner data instead.
        player.level().getEntitiesOfClass(EndCrystal.class,
                        player.getBoundingBox().inflate(EECrystal_HEAL_DISTANCE.get())).stream()
                .filter(c -> c.getEntityData().get(ENDER_EYE_OWNER)
                        .map(owner -> owner.equals(player.getUUID())).orElse(false))
                .forEach(c -> c.getEntityData().set(ENDER_EYE_OWNER, Optional.empty()));
    }
}
