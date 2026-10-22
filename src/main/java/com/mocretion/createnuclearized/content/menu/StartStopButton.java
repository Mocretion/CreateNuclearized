package com.mocretion.createnuclearized.content.menu;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StartStopButton extends AbstractButton {

    private static final int X = 114;
    private static final int Y = 51;
    private static final int WIDTH = 55;
    private static final int HEIGHT = 19;
    private static final int HOVER_U = 200;
    private static final int START_HOVER_V = 0;
    private static final int STOP_V = 20;
    private static final int STOP_HOVER_V = 39;
    private static final int LABEL_COLOR = 0xFFFFFF;

    private static final Component START = Component.translatable(CreateNuclearized.MODID + ".gui.controller.start");
    private static final Component STOP = Component.translatable(CreateNuclearized.MODID + ".gui.controller.stop");

    private final ResourceLocation texture;
    private final ControllerMenu menu;

    public StartStopButton(ResourceLocation texture, int left, int top, ControllerMenu menu) {
        super(left + X, top + Y, WIDTH, HEIGHT, Component.empty());
        this.texture = texture;
        this.menu = menu;
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, ControllerMenu.BUTTON_TOGGLE);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean running = menu.isRunning();
        boolean hovered = isHovered();

        int u = HOVER_U;
        int v;
        if (running) {
            v = hovered ? STOP_HOVER_V : STOP_V;
        } else if (hovered) {
            v = START_HOVER_V;
        } else {
            u = X;
            v = Y;
        }
        graphics.blit(texture, getX(), getY(), u, v, WIDTH, HEIGHT);

        Font font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, running ? STOP : START, getX() + WIDTH / 2,
                getY() + (HEIGHT - font.lineHeight) / 2 + 1, LABEL_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
