package com.unddefined.enderechoing.client.gui.widgets;

import com.unddefined.enderechoing.client.gui.TunerMenu;
import com.unddefined.enderechoing.client.gui.screen.TunerScreen;
import com.unddefined.enderechoing.compat.sculkborne.SculkBorneBridge;
import com.unddefined.enderechoing.network.packet.AddEffectPacket;
import com.unddefined.enderechoing.network.packet.RemoveTeamMemberPacket;
import com.unddefined.enderechoing.network.packet.TeleportRequestPacket;
import com.unddefined.enderechoing.server.DataComponents.EntityData;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static com.unddefined.enderechoing.server.registry.DataRegistry.ENTITY;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_PEARL;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

public class TeamList extends ContainerObjectSelectionList<TeamList.MemberEntry> {
    private final TunerScreen screen;
    private final ContextMenu contextMenu;
    public TunerMenu.Members selectedMember;

    public TeamList(Minecraft minecraft, int width, int height, int x, int y, int itemHeight, TunerScreen screen) {
        super(minecraft, width, height, y, itemHeight);
        this.setX(x);
        this.screen = screen;
        this.contextMenu = new ContextMenu();
        this.selectedMember = null;
    }

    public void addMember(TunerMenu.Members member) {
        this.addEntry(new MemberEntry(this, member));
    }

    public MemberEntry getEntryFromMouse(double mouseX, double mouseY) {
        return this.getEntryAtPosition(mouseX, mouseY);
    }

    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    public void openContextMenu(int mouseX, int mouseY, TunerMenu.Members member, MemberEntry entry) {
        contextMenu.clear();
//        if (!member.isOnline()) return;

        if (screen.getMenu().canWarp() && !entry.isSelf && member.isOnline()){
        contextMenu.enableWarp = screen.getMenu().ee_pearl_amount > 0;
        contextMenu.addItem("screen.enderechoing.warp", () -> {
            screen.onClose();
            PacketDistributor.sendToServer(
                    new TeleportRequestPacket(new GlobalPos(member.dimension(), member.blockPos()), true));
        });}
        contextMenu.addItem("screen.enderechoing.copy", () -> {
            var pearl = new ItemStack(ENDER_ECHOING_PEARL.get(), 1);
            pearl.set(ENTITY, new EntityData(member.uuid()));
            pearl.set(CUSTOM_NAME, Component.literal(member.playerName()));
            screen.getMenu().givePlayerPearl(pearl);
        });
        contextMenu.addItem("screen.enderechoing.remove", () -> {
            contextMenu.addItem("screen.enderechoing.confirm_remove", () -> {
                screen.getMenu().getMembers().remove(member);
                removeEntry(entry);
                PacketDistributor.sendToServer(new RemoveTeamMemberPacket(member.uuid()));
                if (!entry.isSelf) screen.getMenu().ee_pearl_amount++;
            });
            contextMenu.open(mouseX, mouseY, (idx, item) -> {});
        });
        contextMenu.open(mouseX, mouseY, (idx, item) -> {});
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width;
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    public static class MemberEntry extends ContainerObjectSelectionList.Entry<MemberEntry> {
        private static final WidgetSprites SPRITES = new WidgetSprites(
                ResourceLocation.withDefaultNamespace("widget/button"),
                ResourceLocation.withDefaultNamespace("widget/button_disabled"),
                ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        );
        private final Minecraft mc = Minecraft.getInstance();
        private final TeamList parent;
        private final TunerMenu.Members member;
        public boolean selected = false;
        private boolean hovered;
        public final boolean isSelf;
        private final boolean can_crossDimension;
        private final boolean canWarp;
        private final boolean isFacing_down;

        public MemberEntry(TeamList parent, TunerMenu.Members member) {
            this.parent = parent;
            this.member = member;
            var menu = parent.screen.getMenu();
            this.canWarp = menu.canWarp();
            this.isFacing_down = menu.isFacing_down();
            this.can_crossDimension = (menu.getTunerPos().dimension().equals(member.dimension())) || menu.isMultiBlocked();
            this.isSelf = member.uuid().equals(mc.player.getUUID());
        }

        @Override
        public void render(GuiGraphics gfx, int index, int top, int left, int entryWidth, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.hovered = hovered && !parent.getContextMenu().isVisible();
            selected = parent.selectedMember == member;

            gfx.blitSprite(SPRITES.get(true, this.hovered || this.selected), left + 3, top, entryWidth - 10, height);

            Component text = Component.literal(member.playerName())
                    .append(" (").append(Component.translatable(
                            member.isOnline() ? "screen.enderechoing.member_online" : "screen.enderechoing.member_offline"))
                    .append(")");
            int color = member.isOnline() ? 0xE0E0E0 : selected && canSelect() ? 0xFFFFA0 : 0xA8A8A8;
            gfx.drawString(mc.font, text, left + entryWidth / 2 - mc.font.width(text) / 2, top + 6, color, false);
        }

        public void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
            if (!hovered) return;
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(member.playerName()));
            if (member.isOnline()) {
                tooltip.add(Component.translatable("item.enderechoing.ender_echoing_pearl.position",
                        member.blockPos().toShortString(), Component.translationArg(member.dimension().location()))
                        .withColor(canSelect() ? 0xE0E0E0 : 0xA8A8A8));
                tooltip.add(Component.translatable("screen.enderechoing.distance",
                        (int) Math.sqrt(parent.screen.getMenu().getTunerPos().pos().distSqr(member.blockPos())))
                        .withColor(canSelect() ? 0xE0E0E0 : 0xA8A8A8));
            } else tooltip.add(Component.translatable("screen.enderechoing.member_offline"));

            gfx.renderComponentTooltip(mc.font, tooltip, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!hovered) return false;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selected = !selected;
            if (button == 1) {
                selected = true;
                parent.selectedMember = member;
                parent.openContextMenu((int) mouseX, (int) mouseY, member, this);
            }
            if (button == 0 && canSelect() && member.isOnline() && !isSelf) {
                parent.selectedMember = selected ? member : null;
                parent.setSelected(this);
                parent.screen.waypointList.selectedPosition = null;
                var M = new MarkedPositionsManager.MarkedPositions(member.dimension(), member.blockPos(), member.playerName(), 0, false);
                parent.screen.getMenu().setSelectedPosition(M);
                PacketDistributor.sendToServer(new AddEffectPacket(SculkBorneBridge.veilEffect(), 3 * 20, member.uuid()));
            }
            return true;
        }

        private boolean canSelect() {
            return !isFacing_down || (parent.screen.getMenu().ee_pearl_amount > 0 && (can_crossDimension || canWarp));
        }

        public TunerMenu.Members getMember() {
            return member;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }
}
