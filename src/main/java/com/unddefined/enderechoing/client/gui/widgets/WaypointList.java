package com.unddefined.enderechoing.client.gui.widgets;

import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoint;
import com.unddefined.enderechoing.client.gui.screen.PositionEditScreen;
import com.unddefined.enderechoing.client.gui.screen.TunerScreen;
import com.unddefined.enderechoing.network.packet.TeleportRequestPacket;
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

import java.util.List;

import static com.unddefined.enderechoing.server.registry.DataRegistry.ANCHOR_BOUND;
import static com.unddefined.enderechoing.server.registry.DataRegistry.POSITION;
import static com.unddefined.enderechoing.server.registry.ItemRegistry.ENDER_ECHOING_PEARL;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;
import static net.minecraft.core.component.DataComponents.CUSTOM_NAME;

public class WaypointList extends ContainerObjectSelectionList<WaypointList.WaypointEntry> {
    private final TunerScreen screen;
    private final ContextMenu contextMenu;
    public EnderEchoWaypoint selectedPosition;

    public WaypointList(Minecraft minecraft, int width, int height, int x, int y, int itemHeight, TunerScreen screen) {
        super(minecraft, width, height, y, itemHeight);
        this.setX(x);
        this.screen = screen;
        this.contextMenu = new ContextMenu();
        selectedPosition = screen.getWaypointsCache().stream().filter(
                M -> M.pos().equals(screen.getMenu().getSelectedPos().pos())).findFirst().orElse(null);
    }

    public void addWaypoint(EnderEchoWaypoint M) {this.addEntry(new WaypointEntry(this, M));}

    public WaypointEntry getEntryFromMouse(double mouseX, double mouseY) {return this.getEntryAtPosition(mouseX, mouseY);}

    public ContextMenu getContextMenu() {return contextMenu;}

    public void openContextMenu(int mouseX, int mouseY, EnderEchoWaypoint M, WaypointEntry entry) {
        contextMenu.clear();

        if (screen.getMenu().canWarp()){
            contextMenu.enableWarp = screen.getMenu().ee_pearl_amount > 0;
            contextMenu.addItem("screen.enderechoing.warp",() -> {
            screen.onClose();
            PacketDistributor.sendToServer(
                    new TeleportRequestPacket(new GlobalPos(M.dimension(), M.pos()), true));
        });}

        contextMenu.addItem("screen.enderechoing.rename",
                () -> Minecraft.getInstance().setScreen(new PositionEditScreen(screen, M.name(), M.pos())));

        if (!screen.getMenu().canWarp()){
        contextMenu.addItem("screen.enderechoing.copy", () -> {
            screen.getMenu().ee_pearl_amount--;
            var pearl = new ItemStack(ENDER_ECHOING_PEARL.get(), 1);
            pearl.set(POSITION.get(), new GlobalPos(M.dimension(), M.pos()));
            pearl.set(ANCHOR_BOUND.get(), M.anchorBound());
            pearl.set(CUSTOM_NAME, Component.literal(M.name()));
            screen.getMenu().givePlayerPearl(pearl);
        });}

        contextMenu.addItem("screen.enderechoing.remove", () -> {
            contextMenu.addItem("screen.enderechoing.confirm_remove", () -> {
                screen.getWaypointsCache().remove(M);
                removeEntry(entry);
                screen.getMenu().ee_pearl_amount++;
                screen.getMenu().setSelectedPosition(null);
            });
            contextMenu.open(mouseX, mouseY, (idx, item) -> {});
        });
        contextMenu.open(mouseX, mouseY, (idx, item) -> {
            // 回调：菜单项被点击
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedPosition == null) return false;
        List<EnderEchoWaypoint> subList = screen.getWaypointsCache().stream().filter(p -> p.iconIndex() == screen.selectedTab).toList();
        return switch (keyCode) {
            case 265 -> moveInSubList(subList, -1, hasShiftDown()); // ↑
            case 264 -> moveInSubList(subList, 1, hasShiftDown()); // ↓
            default -> super.keyPressed(keyCode, scanCode, modifiers);
        };
    }

    private boolean moveInSubList(List<EnderEchoWaypoint> subList, int dir, boolean extreme) {
        if (subList.isEmpty()) return false;

        int i = subList.indexOf(selectedPosition);
        if (i == -1) return false;

        int targetSubIndex;
        if (extreme) targetSubIndex = (dir < 0) ? 0 : subList.size() - 1;
        else {
            targetSubIndex = i + dir;
            if (targetSubIndex < 0 || targetSubIndex >= subList.size()) return false;
        }

        EnderEchoWaypoint target = subList.get(targetSubIndex);
        swapInMainList(selectedPosition, target);
        return true;
    }

    public void swapInMainList(EnderEchoWaypoint a, EnderEchoWaypoint b) {
        int ia = screen.getWaypointsCache().indexOf(a);
        int ib = screen.getWaypointsCache().indexOf(b);

        if (ia == -1 || ib == -1) return;

        screen.getWaypointsCache().set(ia, b);
        screen.getWaypointsCache().set(ib, a);
    }

    @Override
    protected int getScrollbarPosition() {return this.getX() + this.width;}

    @Override
    public int getRowWidth() {return this.width - 10;}

    public static class WaypointEntry extends ContainerObjectSelectionList.Entry<WaypointEntry> {
        private static final WidgetSprites SPRITES = new WidgetSprites(
                ResourceLocation.withDefaultNamespace("widget/button"),
                ResourceLocation.withDefaultNamespace("widget/button_disabled"),
                ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        );
        private final Minecraft mc = Minecraft.getInstance();
        private final WaypointList parent;
        private final EnderEchoWaypoint waypoint;
        private final boolean isSelf;
        private final boolean can_crossDimension;
        public boolean selected = false;
        private boolean hovered;
        private final boolean canWarp;
        private final boolean isFacing_down;

        public WaypointEntry(WaypointList parent, EnderEchoWaypoint M) {
            this.parent = parent;
            var menu = parent.screen.getMenu();
            this.waypoint = M;
            this.canWarp = menu.canWarp();
            this.isFacing_down = menu.isFacing_down();
            this.isSelf = M.pos().above(2).equals(menu.getTunerPos().pos())
                    ||(canWarp && (menu.canWarp() && menu.getTunerPos().pos().distSqr(M.pos()) < 4)
                    && menu.getTunerPos().dimension().equals(M.dimension()));
            this.can_crossDimension = (menu.getTunerPos().dimension().equals(M.dimension())) || menu.isMultiBlocked();
        }

        @Override
        public void render(GuiGraphics gfx, int index, int top, int left, int entryWidth, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.hovered = hovered && !parent.getContextMenu().isVisible();
            selected = parent.selectedPosition == waypoint;
            int width = entryWidth - 6;

            gfx.blitSprite(SPRITES.get(canSelect(), this.hovered || this.selected), left + 3, top, width - 4, height);

            // ---- 绘制文字 ----
            Component text = Component.literal(waypoint.name());

            int color = selected ? 0xFFFFA0 : 0xE0E0E0;
            gfx.drawString(mc.font, text, left + width / 2 - mc.font.width(text) / 2 + 3, top + 6, color, false);
        }

        public void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
            if (!this.hovered) return;
            var posText = Component.translatable("item.enderechoing.ender_echoing_pearl.position", waypoint.pos().toShortString(), Component.translationArg(waypoint.dimension().location()));
            var distanceText = Component.translatable("screen.enderechoing.distance", (int) Math.sqrt(parent.screen.getMenu().getTunerPos().pos().distSqr(waypoint.pos())));
            gfx.renderComponentTooltip(mc.font, List.of(posText, distanceText), mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!hovered || !canSelect()) return false;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selected = !selected;

            if (button == 1) {
                selected = true;
                parent.openContextMenu((int) mouseX, (int) mouseY, waypoint, this);
            }
            parent.selectedPosition = selected ? waypoint : null;
            parent.setSelected(this);
            parent.screen.getMenu().setSelectedPosition(parent.selectedPosition);
            parent.screen.teamList.selectedMember = null;

            return true;
        }

        private boolean canSelect() {
            return !isSelf && (!isFacing_down
                    || (parent.screen.getMenu().ee_pearl_amount > 0 && (can_crossDimension || canWarp)));
        }

        public EnderEchoWaypoint getWaypoint() {return waypoint;}

        @Override
        public List<? extends GuiEventListener> children() {return List.of();}

        @Override
        public List<? extends NarratableEntry> narratables() {return List.of();}
    }
}
