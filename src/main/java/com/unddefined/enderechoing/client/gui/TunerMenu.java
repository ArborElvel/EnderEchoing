package com.unddefined.enderechoing.client.gui;

import com.unddefined.enderechoing.blocks.entity.EnderEchoTunerBlockEntity;
import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;
import com.unddefined.enderechoing.network.packet.GivePlayerPearlPacket;
import com.unddefined.enderechoing.network.packet.SetSelectedPositionPacket;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.team.PlayerTeam;
import com.unddefined.enderechoing.server.team.TeamManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.unddefined.enderechoing.EnderEchoing.GZERO;
import static com.unddefined.enderechoing.EnderEchoing.TUNER_MENU;
import static com.unddefined.enderechoing.blocks.EnderEchoTunerBlock.CHARGED;
import static com.unddefined.enderechoing.blocks.EnderEchoTunerBlock.FACING;
import static com.unddefined.enderechoing.server.registry.BlockRegistry.ENDER_ECHO_TUNER;
import static com.unddefined.enderechoing.server.registry.DataRegistry.*;
import static net.minecraft.core.registries.BuiltInRegistries.ITEM;

public class TunerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public int ee_pearl_amount;
    public int selected_tuner_tab;
    private boolean multi_blocked = false;
    private boolean tuner_charged = false;
    private boolean facing_down = false;
    private final boolean canWarp;
    private List<ItemStack> iconList = new ArrayList<>();
    private List<MarkedPositionsManager.MarkedPositions> markedPositionsCache;
    private List<Members> members = new ArrayList<>();
    private GlobalPos tunerPos;
    private GlobalPos selectedPos = GZERO;

    public TunerMenu(int c, Inventory i, FriendlyByteBuf buf) {this(c, i, ContainerLevelAccess.NULL, buf);}

    public TunerMenu(int containerId, Inventory playerInv, ContainerLevelAccess A, FriendlyByteBuf buf) {
        super(TUNER_MENU.get(), containerId);
        this.access = A;
        this.selected_tuner_tab = buf.readInt();
        this.ee_pearl_amount = buf.readInt();
        this.tunerPos = buf.readGlobalPos();
        this.selectedPos = buf.readGlobalPos();
        this.multi_blocked = buf.readBoolean();
        this.tuner_charged = buf.readBoolean();
        this.facing_down = buf.readBoolean();
        this.canWarp = buf.readBoolean();
        for (int i = 0; i < 10; i++) this.iconList.add(new ItemStack(ITEM.get(buf.readResourceLocation())));
        this.markedPositionsCache = buf.readList(MarkedPositionsManager.MarkedPositions.STREAM_CODEC);
        this.members = buf.readList(Members.STREAM_CODEC);
    }

    public TunerMenu(int containerId, Inventory playerInv, ContainerLevelAccess A, boolean canWarp) {
        super(TUNER_MENU.get(), containerId);
        this.access = A;
        this.canWarp = canWarp;
        A.execute((level, pos) -> {
            this.selected_tuner_tab = playerInv.player.getData(SELECTED_TUNER_TAB.get());
            this.ee_pearl_amount = playerInv.player.getData(EE_PEARL_AMOUNT.get());
            this.iconList = playerInv.player.getData(ICON_LIST.get());
            var M = playerInv.player.getData(MARKED_POSITIONS_CACHE.get());
            M.checkBounds();
            this.markedPositionsCache = M.markedPositions();
            if (playerInv.player instanceof ServerPlayer S) this.members = collectTeamMembers(S);
            if (level.getBlockEntity(pos) instanceof EnderEchoTunerBlockEntity E){
                this.multi_blocked = E.checkMultiblock();
                tuner_charged = E.getBlockState().getValue(CHARGED);
                facing_down = E.getBlockState().getValue(FACING).equals(Direction.DOWN);
                selectedPos = E.getSelectedPos();
            }
            if (level.getBlockEntity(pos) instanceof WarpPlatformBlockEntity E) selectedPos = E.getSelectedPos();
        });
    }

    private static List<Members> collectTeamMembers(ServerPlayer player) {
        List<Members> list = new ArrayList<>();
        // 玩家自己始终作为第一个成员
        list.add(new Members(player.getUUID(), player.getGameProfile().getName(),
                player.level().dimension(), player.blockPosition(), true));

        PlayerTeam team = TeamManager.teamOf(player.server, player.getUUID());
        if (team == null) return list;
        for (UUID memberId : team.members()) {
            if (memberId.equals(player.getUUID())) continue;
            ServerPlayer online = player.server.getPlayerList().getPlayer(memberId);
            if (online != null) {
                list.add(new Members(memberId, online.getGameProfile().getName(),
                        online.level().dimension(), online.blockPosition(), true));
            } else {
                String name = player.server.getProfileCache()
                        .get(memberId).map(profile -> profile.getName()).orElse("?");
                list.add(new Members(memberId, name, Level.OVERWORLD, BlockPos.ZERO, false));
            }
        }
        return list;
    }

    public void setSelectedPosition(MarkedPositionsManager.MarkedPositions M) {
        if (canWarp && !(Minecraft.getInstance().level.getBlockEntity(tunerPos.pos()) instanceof WarpPlatformBlockEntity)) return;
        if (M == null) PacketDistributor.sendToServer(new SetSelectedPositionPacket(tunerPos.pos(), GZERO, ""));
        else PacketDistributor.sendToServer(new SetSelectedPositionPacket(tunerPos.pos(), new GlobalPos(M.dimension(), M.pos()), M.name()));
    }

    public void givePlayerPearl(ItemStack itemStack) {
        if (itemStack.isEmpty()) return;
        if (itemStack.get(POSITION) == null) ee_pearl_amount -= itemStack.getCount();
        PacketDistributor.sendToServer(new GivePlayerPearlPacket(itemStack));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {return ItemStack.EMPTY;}

    @Override
    public boolean stillValid(Player player) {
        return canWarp || AbstractContainerMenu.stillValid(this.access, player, ENDER_ECHO_TUNER.get());
    }

    public List<ItemStack> getIconList() {return iconList;}

    public GlobalPos getTunerPos() {return tunerPos;}

    public GlobalPos getSelectedPos() {return selectedPos;}

    public Boolean isMultiBlocked() {return multi_blocked;}

    public Boolean isCharged(){return tuner_charged;}

    public boolean isFacing_down() {return facing_down;}

    public boolean canWarp() {return canWarp;}

    public List<MarkedPositionsManager.MarkedPositions> getMarkedPositionsCache() {return markedPositionsCache;}

    public List<Members> getMembers() {return members;}

    public void writeClientSideData(RegistryFriendlyByteBuf buf, GlobalPos pos, Boolean canWarp) {
        buf.writeInt(selected_tuner_tab);
        buf.writeInt(ee_pearl_amount);
        buf.writeGlobalPos(pos);
        buf.writeGlobalPos(selectedPos == null ? GZERO : selectedPos);
        buf.writeBoolean(multi_blocked);
        buf.writeBoolean(tuner_charged);
        buf.writeBoolean(facing_down);
        buf.writeBoolean(canWarp);
        for (ItemStack stack : iconList) buf.writeResourceLocation(ITEM.getKey(stack.getItem()));
        buf.writeCollection(markedPositionsCache, MarkedPositionsManager.MarkedPositions.STREAM_CODEC);
        buf.writeCollection(members, Members.STREAM_CODEC);
    }

    public record Members(UUID uuid, String playerName, ResourceKey<Level> dimension,
                          BlockPos blockPos, boolean isOnline) {
        public static final StreamCodec<FriendlyByteBuf, Members> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC,
                Members::uuid,
                ByteBufCodecs.STRING_UTF8,
                Members::playerName,
                ResourceKey.streamCodec(Registries.DIMENSION),
                Members::dimension,
                BlockPos.STREAM_CODEC,
                Members::blockPos,
                ByteBufCodecs.BOOL,
                Members::isOnline,
                Members::new
        );
    }
}
