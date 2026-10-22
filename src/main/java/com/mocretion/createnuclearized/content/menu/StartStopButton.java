package com.mocretion.createnuclearized.content.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StartStopButton extends AbstractButton {

    private static final int X = 115;
    private static final int Y = 64;
    private static final int WIDTH = 56;
    private static final int HEIGHT = 20;
    private static final int U = 200;
    private static final int START_V = 0;
    private static final int START_HOVER_V = 20;
    private static final int STOP_V = 40;
    private static final int STOP_HOVER_V = 60;

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

        int v;
        if (running)
            v = hovered ? STOP_HOVER_V : STOP_V;
        else
            v = hovered ? START_HOVER_V : START_V;
        graphics.blit(texture, getX(), getY(), U, v, WIDTH, HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
