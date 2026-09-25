package com.unddefined.enderechoing.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.unddefined.enderechoing.client.gui.TunerMenu;
import com.unddefined.enderechoing.client.gui.widgets.TabBar;
import com.unddefined.enderechoing.client.gui.widgets.TeamList;
import com.unddefined.enderechoing.client.gui.widgets.WaypointList;
import com.unddefined.enderechoing.network.packet.ShareToTeamPacket;
import com.unddefined.enderechoing.network.packet.SyncTunerDataPacket;
import com.unddefined.enderechoing.server.DataComponents.MarkedPositionsManager;
import com.unddefined.enderechoing.server.registry.ItemRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.unddefined.enderechoing.compat.jei.EnderEchoJeiPlugin.getItemFromJei;
import static net.minecraft.core.registries.BuiltInRegistries.ITEM;

public class TunerScreen extends AbstractContainerScreen<TunerMenu> {
    private final List<MarkedPositionsManager.MarkedPositions> MarkedPositionsCache;
    private final WidgetSprites ACCEPT_SPRITE = new WidgetSprites(ResourceLocation.withDefaultNamespace("pending_invite/accept_highlighted"), ResourceLocation.withDefaultNamespace("pending_invite/accept"));
    private final WidgetSprites REJECT_SPRITE = new WidgetSprites(ResourceLocation.withDefaultNamespace("pending_invite/reject_highlighted"), ResourceLocation.withDefaultNamespace("pending_invite/reject"));
    private static final ResourceLocation SORT_MANUAL_SPRITE = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/gui/sort_manual.png");
    private static final ResourceLocation SORT_DISTANCE_SPRITE = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/gui/sort_distance.png");
    private static final ResourceLocation SORT_NAME_SPRITE = ResourceLocation.fromNamespaceAndPath("enderechoing", "textures/gui/sort_name.png");
    private final int editBarWidth = 240;
    private final int editBarHeight = 20;
    public int selectedTab = 0;
    public boolean changeIcon = false;
    public ItemStack previousIcon;
    public EditBox itemField;
    public EditBox searchField;
    private ItemStack jeiItem = ItemStack.EMPTY;
    private int editBarX, editBarY;
    public WaypointList waypointList;
    public TeamList teamList;
    private boolean dragging = false;
    private TabBar tabBar;
    private WaypointList.WaypointEntry focusingEntry;
    private boolean isValidString;
    private ImageButton ACCEPT_BUTTON;
    private ImageButton REJECT_BUTTON;
    private ImageButton SortButton;
    private int sortMode = 0;

    public TunerScreen(TunerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.MarkedPositionsCache = menu.getMarkedPositionsCache();
    }

    @Override
    protected void init() {
        super.init();

        selectedTab = menu.selected_tuner_tab;
        int listLeft = this.width / 2 - 107;
        int listWidth = 210;
        int listTop = (int) (this.height * 0.5);
        int listBottom = (int) (this.height * 0.3);
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        editBarX = listLeft + 1;
        editBarY = this.height / 5 - 17;
        tabBar = new TabBar(listLeft + 29, listBottom - 30, this);
        waypointList = new WaypointList(this.minecraft, listWidth, listTop, listLeft, listBottom, 24, this);
        teamList = new TeamList(this.minecraft, listWidth, listTop, listLeft, listBottom, 24, this);

        populateWaypointList();
        populateTeamList();
        this.addWidget(waypointList);
        this.addWidget(teamList);
        waypointList.visible = true;
        teamList.visible = false;

        this.itemField = new EditBox(this.font, editBarX, editBarY, editBarWidth - 66, editBarHeight, Component.translatable("screen.enderechoing.enter_registry_name"));
        this.itemField.setMaxLength(50);
        this.setInitialFocus(this.itemField);
        this.itemField.setTooltip(Tooltip.create(Component.translatable("screen.enderechoing.enter_registry_name")));
        this.addWidget(this.itemField);

        searchField = new EditBox(this.font, editBarX, editBarY, editBarWidth - 51, editBarHeight, Component.literal("search"));
        searchField.setMaxLength(50);
        searchField.setResponder(s -> {populateWaypointList();populateTeamList();});
        this.addWidget(this.searchField);

        SortButton = this.addRenderableWidget(new ImageButton(editBarX + 192, editBarY + 1, 18, 18,
                new WidgetSprites(
                        ResourceLocation.withDefaultNamespace("widget/button"),
                        ResourceLocation.withDefaultNamespace("widget/button_disabled"),
                        ResourceLocation.withDefaultNamespace("widget/button_highlighted")), btn -> {
            sortMode = (sortMode + 1) % 3;
            populateWaypointList();
        }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                guiGraphics.blit(getSortSprite(), getX() + 1, getY() + 1, 0, 0, 16, 16, 16, 16);
            }
        });

        // 添加确定和取消按钮
        ACCEPT_BUTTON = this.addRenderableWidget(new ImageButton(editBarX + 110 + 66, editBarY, 18, 18,
                ACCEPT_SPRITE, btn -> {
            if (!isValidString || menu.getIconList().get(selectedTab).isEmpty()) return;
            changeIcon = false;
        }));

        REJECT_BUTTON = this.addRenderableWidget(new ImageButton(editBarX + 110 + 84, editBarY, 18, 18,
                REJECT_SPRITE, btn -> {
            menu.getIconList().set(selectedTab, previousIcon);
            changeIcon = false;
        }));
    }

    public void populateWaypointList() {
        waypointList.children().clear();
        waypointList.setScrollAmount(0);
        String search = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        MarkedPositionsCache.stream()
                .filter(e -> e.iconIndex() == selectedTab)
                .filter(e -> search.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(search))
                .sorted(getWaypointComparator())
                .forEach(e -> waypointList.addWaypoint(e));
    }

    public void populateTeamList() {
        teamList.children().clear();
        teamList.setScrollAmount(0);
        String search = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        this.getMenu().getMembers().stream()
                .filter(e -> search.isEmpty() || e.playerName().toLowerCase(Locale.ROOT).contains(search))
                .forEach(member -> teamList.addMember(member));
    }

    private Comparator<MarkedPositionsManager.MarkedPositions> getWaypointComparator() {
        Comparator<MarkedPositionsManager.MarkedPositions> comparator = Comparator.comparing(this::isSelfWaypoint).reversed();
        if (sortMode == 1) {
            comparator = comparator.thenComparingDouble(e -> e.pos().distSqr(menu.getTunerPos().pos()));
        } else if (sortMode == 2) {
            comparator = comparator.thenComparing(e -> e.name().toLowerCase(Locale.ROOT));
        }
        return comparator;
    }

    private ResourceLocation getSortSprite() {
        return switch (sortMode) {
            case 1 -> SORT_DISTANCE_SPRITE;
            case 2 -> SORT_NAME_SPRITE;
            default -> SORT_MANUAL_SPRITE;
        };
    }

    private boolean isSelfWaypoint(MarkedPositionsManager.MarkedPositions M) {
        return M.pos().above(2).equals(menu.getTunerPos().pos())
                || (menu.canWarp() && (menu.getTunerPos().pos().distSqr(M.pos()) < 4)
                && menu.getTunerPos().dimension().equals(M.dimension()));
    }

    @Override
    public void onClose() {
        if (menu.getIconList().get(selectedTab).isEmpty()) menu.getIconList().set(selectedTab, previousIcon);
        PacketDistributor.sendToServer(new SyncTunerDataPacket(menu.getIconList(), MarkedPositionsCache, menu.ee_pearl_amount));
        if (!menu.isFacing_down() && !menu.canWarp()) menu.setSelectedPosition(null);
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchField.render(guiGraphics, mouseX, mouseY, partialTick);
        this.itemField.render(guiGraphics, mouseX, mouseY, partialTick);
        SortButton.render(guiGraphics, mouseX, mouseY, partialTick);

        waypointList.render(guiGraphics, mouseX, mouseY, partialTick);
        teamList.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染waypoint列表项的tooltip，避免被列表边框截断
        var hoveredEntry = waypointList.getEntryFromMouse(mouseX, mouseY);
        if (hoveredEntry != null) hoveredEntry.renderTooltip(guiGraphics, mouseX + 5, mouseY + 5);
        var hoveredMemberEntry = teamList.getEntryFromMouse(mouseX, mouseY);
        if (hoveredMemberEntry != null) hoveredMemberEntry.renderTooltip(guiGraphics, mouseX + 5, mouseY + 5);

        tabBar.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染标题
        guiGraphics.drawString(this.font, this.title, width / 2 + 2 - (this.font.width(this.title) / 2),
                this.height / 7 - 11, 0xd1d6b6, false);

        waypointList.getContextMenu().render(guiGraphics, mouseX, mouseY, partialTick);
        teamList.getContextMenu().render(guiGraphics, mouseX, mouseY, partialTick);
        tabBar.getContextMenu().render(guiGraphics, mouseX, mouseY, partialTick);
        if (dragging) guiGraphics.renderFakeItem(new ItemStack(ItemRegistry.ENDER_ECHOING_PEARL.get()), mouseX - 8, mouseY - 8);
        if (changeIcon && !jeiItem.isEmpty()) guiGraphics.renderFakeItem(jeiItem, mouseX - 8, mouseY - 8);
    }

    @Override
    public void containerTick() {
        tabBar.selectedTab = selectedTab;
        tabBar.ee_pearl_amount = menu.ee_pearl_amount;
        if (waypointList.getMaxScroll() > 0) waypointList.setWidth(204);
        else waypointList.setWidth(210);
        imageWidth = changeIcon ? this.width / 4 : width;
        this.itemField.visible = changeIcon;
        this.searchField.visible = !changeIcon;
        SortButton.visible = !changeIcon;
        ACCEPT_BUTTON.visible = changeIcon;
        REJECT_BUTTON.visible = changeIcon;
        if (selectedTab == 0) changeIcon = false;
        if (!changeIcon) return;

        String value = this.itemField.getValue();
        isValidString = Pattern.compile("[a-z0-9:_.-]+").matcher(value).matches();
        if (isValidString) {
            var location = ResourceLocation.tryParse(value);
            if (location != null) menu.getIconList().set(selectedTab, ITEM.get(location).getDefaultInstance());
        }
        this.itemField.setTextColor(!isValidString || menu.getIconList().get(selectedTab).isEmpty() ? 0xFF0000 : 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!changeIcon && button == 1 && searchField.isMouseOver(mouseX, mouseY)) {
            searchField.setValue("");
            searchField.setFocused(true);
            setFocused(searchField);
            return true;
        }
        if (changeIcon && button == 1 && itemField.isMouseOver(mouseX, mouseY)) {
            itemField.setValue("");
            itemField.setFocused(true);
            setFocused(itemField);
            return true;
        }

        if (!changeIcon && !searchField.isMouseOver(mouseX, mouseY)) searchField.setFocused(false);
        if (!changeIcon && !SortButton.isMouseOver(mouseX, mouseY)) SortButton.setFocused(false);


        if (changeIcon && jeiItem.isEmpty() && !(mouseX >= editBarX && mouseX < editBarX + editBarWidth && mouseY >= editBarY && mouseY < editBarY + editBarHeight)) {
            menu.getIconList().set(selectedTab, previousIcon);
            changeIcon = false;
        }

        if (tabBar.getContextMenu().isVisible() && tabBar.getContextMenu().mouseClicked(mouseX, mouseY, button)) return true;

        if (waypointList.getContextMenu().isVisible() && waypointList.getContextMenu().mouseClicked(mouseX, mouseY, button)) return true;
        if (teamList.getContextMenu().isVisible() && teamList.getContextMenu().mouseClicked(mouseX, mouseY, button)) return true;

        focusingEntry = waypointList.getEntryFromMouse(mouseX, mouseY);
        if (focusingEntry != null && focusingEntry.mouseClicked(mouseX, mouseY, button)) return true;

        var hoveredMemberEntry = teamList.getEntryFromMouse(mouseX, mouseY);
        if (hoveredMemberEntry != null && hoveredMemberEntry.mouseClicked(mouseX, mouseY, button)) return true;

        if (tabBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (selectedTab == 9) {
            waypointList.visible = false;
            teamList.visible = true;
        } else {
            waypointList.visible = true;
            teamList.visible = false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (changeIcon && !jeiItem.isEmpty() && (mouseX >= editBarX && mouseX < editBarX + editBarWidth && mouseY >= editBarY && mouseY < editBarY + editBarHeight))
            itemField.setValue(ITEM.getKey(jeiItem.getItem()).toString());
        jeiItem = ItemStack.EMPTY;
        if (tabBar.mouseReleased(mouseX, mouseY, button)) return true;
        if (!dragging) return true;
        this.dragging = false;
        if (waypointList.getSelected() == null) return true;
        if (button != 0) return true;

        var M = waypointList.getSelected().getMarkedPosition();
        waypointList.setSelected(null);
        // 更换图标
        for (int i = 0; i <= 9; i++) {
            double slotSize = 20;
            double tx = (double) this.width / 2 - 81 - 30 + i * slotSize + (i > 0 ? 9 : 0);
            double ty = (int) (this.height * 0.3) - 30;

            if (!(mouseX >= tx && mouseX < tx + slotSize && mouseY >= ty && mouseY < ty + slotSize)) continue;

            if (i == 9) {
                PacketDistributor.sendToServer(new ShareToTeamPacket(M));
                return super.mouseReleased(mouseX, mouseY, button);
            }

            selectedTab = i;

            var newPosition = new MarkedPositionsManager.MarkedPositions(M.dimension(), M.pos(), M.name(), i, M.teleporterBound());
            MarkedPositionsCache.set(MarkedPositionsCache.indexOf(M), newPosition);
            populateWaypointList();
            return super.mouseReleased(mouseX, mouseY, button);
        }
        // 交换位置
        var swapEntry = waypointList.getEntryFromMouse(mouseX, mouseY);
        if (swapEntry != null && swapEntry != focusingEntry && sortMode == 0) {
            waypointList.swapInMainList(M, swapEntry.getMarkedPosition());
            populateWaypointList();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (changeIcon && jeiItem.isEmpty()) jeiItem = getItemFromJei();
        if (waypointList.getContextMenu().isVisible() || tabBar.getContextMenu().isVisible()) return false;
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (tabBar.mouseDragged(button)) return true;

        var selected = waypointList.getSelected();
        if (selected == null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (!dragging && focusingEntry != null && focusingEntry.equals(selected)) this.dragging = true;

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (changeIcon && keyCode == InputConstants.KEY_E) return false;
        if (changeIcon && keyCode == InputConstants.KEY_RETURN) {
            this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            if (!isValidString || menu.getIconList().get(selectedTab).isEmpty()) return false;
            changeIcon = false;
        }
        if (!changeIcon && tabBar.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (!changeIcon && waypointList.keyPressed(keyCode, scanCode, modifiers)) {
            populateWaypointList();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 渲染GUI背景纹理
//        guiGraphics.blit(
//                ResourceLocation.withDefaultNamespace("gui/menu_background"),
//                this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    public WaypointList.WaypointEntry getFocusingEntry() {
        return focusingEntry;
    }

    public List<MarkedPositionsManager.MarkedPositions> getMarkedPositionsCache() {
        return MarkedPositionsCache;
    }

}
