package com.unddefined.enderechoing.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.unddefined.enderechoing.client.gui.widgets.DimensionSelecter;
import com.unddefined.enderechoing.client.gui.widgets.TabBar;
import com.unddefined.enderechoing.network.packet.PearlRenamePacket;
import com.unddefined.enderechoing.network.packet.RequestDimensionListPacket;
import com.unddefined.enderechoing.network.packet.RequestStructureInfoPacket;
import com.unddefined.enderechoing.network.packet.SetUnchargedPacket;
import com.unddefined.enderechoing.api.waypoint.EnderEchoWaypoint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PositionEditScreen extends Screen {
    private final Screen lastScreen;
    private final BlockPos pos;
    public String fieldValue;
    public Font font;
    private EditBox nameField;
    public TabBar tabBar;
    private EditBox posX;
    private EditBox posY;
    private EditBox posZ;
    private DimensionSelecter DimensionSelecter;
    private boolean CursorMoved = false;
    private boolean insertVisible = false;
    private boolean isCharged = false;
    private String dimension;
    private String biome;
    private String structure = "loading...";
    private Button structureBtn;
    private Button dimBtn;
    private Button biomeBtn;
    private Button doneBtn;
    private Button cancelBtn;

    public PositionEditScreen(Screen lastScreen, String fieldValue, BlockPos pos) {
        super(Component.translatable("screen.enderechoing.edit_title"));
        this.lastScreen = lastScreen;
        this.fieldValue = fieldValue;
        this.pos = pos;
    }

    @Override
    protected void init() {
        if (fieldValue.contains("÷")) {
            fieldValue = fieldValue.replace("÷", "");
            tabBar = new TabBar(width / 2 - 76, height / 2 - 38, this);
        }
        // 添加文本输入框
        this.font = super.font;
        this.nameField = new EditBox(this.font, this.width / 2 - 105, this.height / 2 - 10, 188, 20, Component.translatable("screen.enderechoing.enter_name"));
        this.nameField.setMaxLength(50);
        this.nameField.setValue(fieldValue);
        this.addWidget(this.nameField);
        this.setInitialFocus(this.nameField);
        if (!fieldValue.equals("><")) CursorMoved = true;
        if (this.minecraft != null) {
            var player = this.minecraft.player;
            if (player != null) {
                dimension = player.level().dimension().location().toShortLanguageKey();
                biome = player.level().getBiome(player.blockPosition()).getRegisteredName();

                PacketDistributor.sendToServer(new RequestStructureInfoPacket(pos));
                PacketDistributor.sendToServer(new RequestDimensionListPacket());
            }
        }
        // 添加插入按钮
        var insertBtn = Button.builder(Component.literal("+"), (button) -> insertVisible = !insertVisible)
                .bounds(this.width / 2 + 86, this.height / 2 - 10, 20, 20).build();
        insertBtn.setTooltip(Tooltip.create(Component.translatable("screen.enderechoing.insert_info")));
        this.addRenderableWidget(insertBtn);
        if (lastScreen instanceof TunerScreen tunerScreen) {
            //需处于同一维度
            var M = tunerScreen.getFocusingEntry().getWaypoint();
            if (M != null && !M.dimension().location().toShortLanguageKey().equals(dimension)) insertBtn.active = false;
        }

        if (lastScreen instanceof TunerScreen tunerScreen && tunerScreen.getMenu().isCharged()) {
            isCharged = true;
            var M = tunerScreen.getFocusingEntry().getWaypoint();
            posX = new EditBox(this.font, this.width / 2 - 90, this.height / 2 + 16, 52, 20, Component.literal(String.valueOf(M.pos().getX())));
            posX.setValue(M.pos().getX() + "");
            posY = new EditBox(this.font, this.width / 2 - 21, this.height / 2 + 16, 52, 20, Component.literal(String.valueOf(M.pos().getY())));
            posY.setValue(M.pos().getY() + "");
            posZ = new EditBox(this.font, this.width / 2 + 50, this.height / 2 + 16, 52, 20, Component.literal(String.valueOf(M.pos().getZ())));
            posZ.setValue(M.pos().getZ() + "");
            DimensionSelecter = new DimensionSelecter(List.of(Level.OVERWORLD), this, this.width / 2 - 101, this.height / 2 + 41, 210, M.dimension().location().toString());
            DimensionSelecter.dimension = M.dimension();
            this.addWidget(posX);
            this.addWidget(posY);
            this.addWidget(posZ);
            this.addWidget(DimensionSelecter);
        }

        // 添加维度按钮
        dimBtn = Button.builder(Component.translatable("screen.enderechoing.dimension"), (button) ->
                        this.nameField.insertText(dimension != null ? dimension : "null"))
                .bounds(this.width / 2 - 101, this.height / 2 + 70 - (isCharged ? 41 - 70 : 0), 200, 20).build();
        dimBtn.setTooltip(Tooltip.create(Component.translatable(dimension != null ? dimension : "null")));
        this.addRenderableWidget(dimBtn);

        // 添加生物群系按钮
        biomeBtn = Button.builder(Component.translatable("screen.enderechoing.biome"), (button) ->
                        this.nameField.insertText(biome != null ? biome : "null"))
                .bounds(this.width / 2 - 101, this.height / 2 + 93 - (isCharged ? 41 - 73 : 0), 200, 20).build();
        biomeBtn.setTooltip(Tooltip.create(Component.translatable(biome != null ? biome : "null")));
        this.addRenderableWidget(biomeBtn);

        // 添加结构按钮
        structureBtn = Button.builder(Component.translatable("screen.enderechoing.structure"), (button) ->
                        this.nameField.insertText(structure != null ? structure : "null"))
                .bounds(this.width / 2 - 101, this.height / 2 + 116 - (isCharged ? 41 - 76 : 0), 200, 20).build();
        structureBtn.setTooltip(Tooltip.create(Component.translatable(structure != null ? structure : "null")));
        this.addRenderableWidget(structureBtn);

        // 添加完成按钮
        this.doneBtn = Button.builder(Component.translatable("screen.enderechoing.done"), (button) ->
                this.onDone()).bounds(this.width / 2 - 106, this.height / 2 + 20 + (isCharged ? 43 : 0), 100, 20).build();
        this.addRenderableWidget(doneBtn);
        // 添加取消按钮
        this.cancelBtn = Button.builder(Component.translatable("screen.enderechoing.cancel"), (button) ->
                this.onClose()).bounds(this.width / 2 + 6, this.height / 2 + 20 + (isCharged ? 43 : 0), 100, 20).build();
        this.addRenderableWidget(cancelBtn);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int tab = tabBar != null ? 24 : 0;

        // 渲染标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 30 - tab, 0xFFFFFF);

        if (tabBar != null) tabBar.render(guiGraphics, mouseX, mouseY, partialTick);
        this.nameField.render(guiGraphics, mouseX, mouseY, partialTick);
        if (posX != null) {
            posX.render(guiGraphics, mouseX, mouseY, partialTick);
            posY.render(guiGraphics, mouseX, mouseY, partialTick);
            posZ.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawString(font, "X:", this.width / 2 - 101, this.height / 2 + 22, -1, false);
            guiGraphics.drawString(font, "Y:", this.width / 2 - 34, this.height / 2 + 22, -1, false);
            guiGraphics.drawString(font, "Z:", this.width / 2 + 36, this.height / 2 + 22, -1, false);
            DimensionSelecter.render(guiGraphics, mouseX, mouseY, partialTick);
            if (DimensionSelecter.isVisible()) {
                doneBtn.visible = false;
                cancelBtn.visible = false;
            } else {
                doneBtn.visible = true;
                cancelBtn.visible = true;
            }
        }

        if (!CursorMoved) this.nameField.moveCursorTo(1, false);
        CursorMoved = true;
        if (insertVisible) {
            this.dimBtn.visible = true;
            this.biomeBtn.visible = true;
            this.structureBtn.visible = true;
        } else {
            this.dimBtn.visible = false;
            this.biomeBtn.visible = false;
            this.structureBtn.visible = false;
        }
    }

    private void onDone() {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty()) name = fieldValue;

        if (lastScreen instanceof TunerScreen tunerScreen) {
            var M = tunerScreen.getFocusingEntry().getWaypoint();
            var newPos = isCharged ? new BlockPos(Integer.parseInt(posX.getValue()), Integer.parseInt(posY.getValue()), Integer.parseInt(posZ.getValue())) : M.pos();
            var newDimension = isCharged ? DimensionSelecter.dimension : M.dimension();
            var newM = new EnderEchoWaypoint(newDimension, newPos, name,
                    tabBar != null ? tabBar.selectedTab : M.iconIndex(), M.anchorBound());
            tunerScreen.getWaypointsCache().set(tunerScreen.getWaypointsCache().indexOf(M), newM);
            tunerScreen.populateWaypointList();
            if (isCharged && !newPos.equals(M.pos()) || !newDimension.equals(M.dimension()))
                PacketDistributor.sendToServer(new SetUnchargedPacket(tunerScreen.getMenu().getTunerPos().pos()));
        } else PacketDistributor.sendToServer(new PearlRenamePacket(name, tabBar != null ? tabBar.selectedTab : -1));


        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabBar != null) tabBar.mouseClicked(mouseX, mouseY, button);
        if (tabBar != null) if (tabBar.selectedTab == 9) tabBar.selectedTab = 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN) {
            this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            this.onDone();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        CursorMoved = false;
        if (this.minecraft != null) this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 供 ReplyStructureInfoPacket 回调使用
    public void setStructure(String structure) {
        this.structure = structure;
        if (this.structureBtn != null) this.structureBtn.setTooltip(Tooltip.create(Component.translatable(structure != null ? structure : "null")));
    }

    public void setDimensionList(List<ResourceKey<Level>> dimensionList) {
        if (this.DimensionSelecter != null && dimensionList != null) this.DimensionSelecter.updateDimensionList(dimensionList);
    }
}
