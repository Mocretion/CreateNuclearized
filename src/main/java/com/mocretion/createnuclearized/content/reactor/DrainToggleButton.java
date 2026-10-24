package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.menu.CNIcons;
import com.simibubi.create.foundation.gui.AllIcons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DrainToggleButton extends AbstractButton {

    private static final int X = 56;
    private static final int Y = 64;
    private static final int WIDTH = 56;
    private static final int HEIGHT = 18;
    private static final int U = 200;
    private static final int IDLE_V = 0;
    private static final int HOVER_V = 18;
    private static final int LOCKED_V = 72;

    private static final int SELECTED_COLOR = 0x47644D;
    private static final int UNSELECTED_COLOR = 0xA0A0A0;
    private static final float RUNNING_DIM = 0.8f;

    private static final int FUEL_ICON_X = 12;
    private static final int SPENT_ICON_X = 28;
    private static final int ICON_Y = (HEIGHT - 16) / 2;

    private final ResourceLocation texture;
    private final ReactorControllerMenu menu;

    public DrainToggleButton(ResourceLocation texture, int left, int top, ReactorControllerMenu menu) {
        super(left + X, top + Y, WIDTH, HEIGHT, Component.empty());
        this.texture = texture;
        this.menu = menu;
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, ReactorControllerMenu.BUTTON_TOGGLE_DRAIN);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean running = menu.isRunning();
        this.active = !running;
        setTooltip(running ? Tooltip.create(label("swap.locked")) : null);

        int v;
        if (running)
            v = LOCKED_V;
        else
            v = isHovered() ? HOVER_V : IDLE_V;
        graphics.blit(texture, getX(), getY(), U, v, WIDTH, HEIGHT);

        int fuel = menu.getFuelDrainCount();
        int spent = menu.getSpentDrainCount();
        boolean fuelActive = fuel > 0;
        boolean spentActive = spent > 0;
        if (!fuelActive && !spentActive)
            fuelActive = spentActive = true;

        drawIcon(graphics, CNIcons.BLOCK_INPUT, FUEL_ICON_X, fuelActive, running);
        drawIcon(graphics, AllIcons.I_PRIORITY_LOW, SPENT_ICON_X, spentActive, running);
    }

    private void drawIcon(GuiGraphics graphics, AllIcons icon, int localX, boolean selected, boolean running) {
        int color = selected ? SELECTED_COLOR : UNSELECTED_COLOR;
        if (running)
            color = dim(color, RUNNING_DIM);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        graphics.setColor(r, g, b, 1f);
        icon.render(graphics, getX() + localX, getY() + ICON_Y);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static int dim(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return !menu.isRunning() && super.isValidClickButton(button);
    }

    private static Component label(String key) {
        return Component.translatable(CreateNuclearized.MODID + ".gui.controller." + key);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
