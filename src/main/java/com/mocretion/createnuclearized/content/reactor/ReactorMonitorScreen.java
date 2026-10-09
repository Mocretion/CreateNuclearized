package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class ReactorMonitorScreen extends AbstractContainerScreen<ReactorMonitorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/reactor_monitor.png");

    private static final int WINDOW_U = 0;
    private static final int WINDOW_V = 65;
    private static final int WINDOW_W = 176;
    private static final int WINDOW_H = 191;

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
    private static final int STATUS_Y = TAB_HEIGHT + 6;
    private static final int CONTENT_Y = TAB_HEIGHT + 20;

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
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ReactorState state = menu.getState();
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
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0)
            graphics.renderTooltip(this.font, MonitorTab.values()[tab].title(), mouseX, mouseY);
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

    static String formatDuration(int totalSeconds) {
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }
}
