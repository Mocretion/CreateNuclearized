package com.mocretion.createnuclearized.content.turbine;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TurbineControllerScreen extends AbstractContainerScreen<TurbineControllerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/reactor_controller.png");

    private static final int BUTTON_X = 114;
    private static final int BUTTON_Y = 51;
    private static final int BUTTON_W = 55;
    private static final int BUTTON_H = 19;

    private static final int LINE_X = 8;
    private static final int FIRST_LINE_Y = 18;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0x404040;

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        addRenderableWidget(new StartStopButton(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, this.menu));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        TurbineState state = menu.getState();
        graphics.drawString(this.font, state.label(), this.titleLabelX, this.titleLabelY, state.color(), false);

        drawLine(graphics, 0, "Steam: " + menu.getSteamBuffered() + " / " + menu.getSteamCapacity() + " mB");
        drawLine(graphics, 1, "Steam Use: " + menu.getSteamUse() + " mB/t");
        drawLine(graphics, 2, "Stages: " + menu.getStages() + ", Blade Blocks: " + menu.getBladeCells());
        drawLine(graphics, 3, "Output: " + menu.getOutputPercent() + "%");
        drawLine(graphics, 4, "Speed: " + menu.getGeneratedRpm() + " RPM");

        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, TEXT_COLOR, false);
    }

    private void drawLine(GuiGraphics graphics, int line, String text) {
        graphics.drawString(this.font, text, LINE_X, FIRST_LINE_Y + line * LINE_HEIGHT, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private static class StartStopButton extends AbstractButton {

        private final TurbineControllerMenu menu;

        StartStopButton(int x, int y, TurbineControllerMenu menu) {
            super(x, y, BUTTON_W, BUTTON_H, Component.empty());
            this.menu = menu;
        }

        @Override
        public void onPress() {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(
                    menu.containerId, TurbineControllerMenu.BUTTON_TOGGLE);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean running = menu.isRunning();
            boolean hovered = isHovered();

            int u;
            int v;
            String label;
            if (running) {
                label = "stop";
                u = 200;
                v = hovered ? 39 : 20;
            } else {
                label = "start";
                if (hovered) {
                    u = 200;
                    v = 0;
                } else {
                    u = BUTTON_X;
                    v = BUTTON_Y;
                }
            }

            graphics.blit(TEXTURE, getX(), getY(), u, v, BUTTON_W, BUTTON_H);

            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, label, getX() + BUTTON_W / 2,
                    getY() + (BUTTON_H - font.lineHeight) / 2 + 1, 0xFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
