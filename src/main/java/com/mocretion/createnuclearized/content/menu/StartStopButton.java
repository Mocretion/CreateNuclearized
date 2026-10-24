package com.mocretion.createnuclearized.content.menu;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StartStopButton extends AbstractButton {

    private static final int LABEL_COLOR = 0xFFFFFF;

    private static final int X = 115;
    private static final int Y = 64;
    private static final int WIDTH = 56;
    private static final int HEIGHT = 18;
    private static final int U = 200;
    private static final int START_V = 0;
    private static final int START_HOVER_V = 18;
    private static final int STOP_V = 36;
    private static final int STOP_HOVER_V = 54;

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

        Component label = Component.translatable(CreateNuclearized.MODID + ".gui.controller." + (running ? "stop" : "start"));
        graphics.drawCenteredString(Minecraft.getInstance().font, label, getX() + WIDTH / 2, getY() + (HEIGHT - 8) / 2, LABEL_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
