package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.multiblock.MultiblockErrorClient;
import com.mocretion.createnuclearized.content.multiblock.MultiblockState;
import com.mocretion.createnuclearized.registry.CNBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class ReactorMonitorScreen extends AbstractContainerScreen<ReactorMonitorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/reactor_monitor.png");
    private static final ResourceLocation REACTOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/81_inventory_grid.png");

    private static final int WINDOW_U = 0;
    private static final int WINDOW_V = 47;
    private static final int WINDOW_W = 176;
    private static final int WINDOW_H = 209;

    private static final int PANEL_U = 0;
    private static final int PANEL_V = 0;
    private static final int PANEL_W = 162;
    private static final int PANEL_H = 162;
    private static final int PANEL_X = 7;
    private static final int PANEL_Y = 44;

    private static final int INTERIOR_BG_U = 0;
    private static final int INTERIOR_BG_V = 162;
    private static final int INTERIOR_BG_W = 166;
    private static final int INTERIOR_BG_H = 92;
    private static final int INTERIOR_BG_X = 5;
    private static final int INTERIOR_BG_Y = 30;

    private static final int GRID = 9;
    private static final int CELL = 18;
    private static final int ICON_V = 0;
    private static final int VALVE_ICON_SIZE = 18;
    private static final int FILLER_ICON_SIZE = 16;
    private static final int FILLER_INSET = 1;
    private static final int IDLE_ICON_U = 186;
    private static final int ACTIVE_ICON_U = 204;
    private static final int INACTIVE_ICON_U = 222;
    private static final int FILLER_ICON_U = 240;

    private static final int TAB_STRIP_X = 9;
    private static final int TAB_WIDTH = 25;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_TOP = 0;

    private static final int ACTIVE_U = 0;
    private static final int ACTIVE_V = 0;
    private static final int ACTIVE_W = 30;
    private static final int ACTIVE_H = 32;
    private static final int ACTIVE_Y_OFFSET = 4;

    private static final int CONTENT_X = 8;
    private static final int STATUS_Y = TAB_HEIGHT + 9;
    private static final int CONTENT_Y = TAB_HEIGHT + 22;

    private int activeTab;

    public ReactorMonitorScreen(ReactorMonitorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WINDOW_W;
        this.imageHeight = WINDOW_H;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, WINDOW_U, WINDOW_V, WINDOW_W, WINDOW_H);

        int tabCenterX = this.leftPos + tabSlotX(activeTab) + TAB_WIDTH / 2;
        int overlayX = tabCenterX - ACTIVE_W / 2;
        int overlayY = this.topPos + TAB_TOP + TAB_HEIGHT - ACTIVE_H + ACTIVE_Y_OFFSET;
        graphics.blit(TEXTURE, overlayX, overlayY, ACTIVE_U, ACTIVE_V, ACTIVE_W, ACTIVE_H);

        if (isInteriorTab()) {
            graphics.blit(REACTOR_TEXTURE, this.leftPos + INTERIOR_BG_X, this.topPos + INTERIOR_BG_Y,
                    INTERIOR_BG_U, INTERIOR_BG_V, INTERIOR_BG_W, INTERIOR_BG_H);
            graphics.blit(REACTOR_TEXTURE, this.leftPos + PANEL_X, this.topPos + PANEL_Y, PANEL_U, PANEL_V, PANEL_W, PANEL_H);
            renderGridOverlays(graphics, clientGrid());
        }
    }

    private void renderGridOverlays(GuiGraphics graphics, InteriorGrid grid) {
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                if (col >= grid.width() || row >= grid.depth())
                    drawFiller(graphics, col, row);
            }
        }
        for (InteriorGrid.Cell cell : grid.cells())
            drawValve(graphics, cell.col(), cell.row(), iconU(cell.state()));
    }

    private static int iconU(InteriorGrid.ValveState state) {
        return switch (state) {
            case ACTIVE -> ACTIVE_ICON_U;
            case FULL -> IDLE_ICON_U;
            case INACTIVE -> INACTIVE_ICON_U;
        };
    }

    private void drawValve(GuiGraphics graphics, int col, int row, int u) {
        int x = this.leftPos + PANEL_X + col * CELL;
        int y = this.topPos + PANEL_Y + row * CELL;
        graphics.blit(REACTOR_TEXTURE, x, y, u, ICON_V, VALVE_ICON_SIZE, VALVE_ICON_SIZE);
    }

    private void drawFiller(GuiGraphics graphics, int col, int row) {
        int x = this.leftPos + PANEL_X + col * CELL + FILLER_INSET;
        int y = this.topPos + PANEL_Y + row * CELL + FILLER_INSET;
        graphics.blit(REACTOR_TEXTURE, x, y, FILLER_ICON_U, ICON_V, FILLER_ICON_SIZE, FILLER_ICON_SIZE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        MultiblockState state = menu.getState();
        graphics.drawString(this.font, state.label(), CONTENT_X, STATUS_Y, state.color(), false);

        MonitorTab[] tabs = MonitorTab.values();
        tabs[activeTab].renderContent(graphics, this.font, this.menu, CONTENT_X, CONTENT_Y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = tabAt(mouseX, mouseY);
            if (tab >= 0) {
                if (tab != activeTab) {
                    activeTab = tab;
                    playClick();
                }
                return true;
            }
            if (isInteriorTab()) {
                InteriorGrid grid = clientGrid();
                InteriorGrid.Cell cell = cellUnderMouse(grid, mouseX, mouseY);
                if (cell != null) {
                    MultiblockErrorClient.show(cell.valvePos(grid.floorOrigin()));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.menu.setInventoryVisible(!isInteriorTab());
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0)
            graphics.renderTooltip(this.font, MonitorTab.values()[tab].title(), mouseX, mouseY);

        if (isInteriorTab()) {
            InteriorGrid grid = clientGrid();
            InteriorGrid.Cell cell = cellUnderMouse(grid, mouseX, mouseY);
            if (cell != null)
                graphics.renderComponentTooltip(this.font, valveTooltip(grid, cell), mouseX, mouseY);
        }
    }

    private InteriorGrid clientGrid() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.getPos()) instanceof ReactorMonitorBlockEntity be)
            return be.getGrid();
        return InteriorGrid.EMPTY;
    }

    @Nullable
    private InteriorGrid.Cell cellUnderMouse(InteriorGrid grid, double mouseX, double mouseY) {
        int relX = (int) (mouseX - (this.leftPos + PANEL_X));
        int relY = (int) (mouseY - (this.topPos + PANEL_Y));
        if (relX < 0 || relY < 0 || relX >= PANEL_W || relY >= PANEL_H) return null;
        int col = relX / CELL;
        int row = relY / CELL;
        for (InteriorGrid.Cell cell : grid.cells())
            if (cell.col() == col && cell.row() == row) return cell;
        return null;
    }

    private List<Component> valveTooltip(InteriorGrid grid, InteriorGrid.Cell cell) {
        List<Component> lines = new ArrayList<>();
        lines.add(CNBlocks.REACTOR_SALT_OUTPUT_VALVE.get().getName());
        ReactorFuel fuel = grid.fuel();
        ReactorStats.Channel channel =
                new ReactorStats.Channel(BlockPos.ZERO, cell.burnShift(), cell.purityShift(), cell.rate());
        SaltStats stats = ReactorStats.composition(List.of(channel), fuel, grid.reflectorPercent());
        SaltStatsTooltip.append(lines, stats, fuel);
        SaltStatsTooltip.appendChannel(lines, channel.burn(fuel), channel.purity(fuel), cell.rate(), fuel);
        lines.add(label("modifiers"));
        if (cell.modifiers().isEmpty())
            lines.add(label("no_modifiers"));
        else
            for (InteriorGrid.Mod mod : cell.modifiers())
                lines.add(mod.displayName());
        return lines;
    }

    private static Component label(String key) {
        return Component.translatable(CreateNuclearized.MODID + ".gui.reactor_monitor." + key);
    }

    private boolean isInteriorTab() {
        return activeTab == MonitorTab.INTERIOR.ordinal();
    }

    private static int tabSlotX(int index) {
        return TAB_STRIP_X + index * TAB_WIDTH;
    }

    private int tabAt(double mouseX, double mouseY) {
        int relX = (int) (mouseX - this.leftPos) - TAB_STRIP_X;
        int relY = (int) (mouseY - this.topPos);
        if (relX < 0 || relY < TAB_TOP || relY >= TAB_TOP + TAB_HEIGHT) return -1;
        int index = relX / TAB_WIDTH;
        return index < MonitorTab.values().length ? index : -1;
    }

    static Component formatDuration(int totalSeconds) {
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return Component.translatable(CreateNuclearized.MODID + ".gui.duration", days, hours, minutes, seconds);
    }
}
