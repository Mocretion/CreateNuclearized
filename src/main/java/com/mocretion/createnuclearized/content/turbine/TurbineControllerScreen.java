package com.mocretion.createnuclearized.content.turbine;

import com.mocretion.createnuclearized.content.menu.ControllerScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TurbineControllerScreen extends ControllerScreen<TurbineControllerMenu> {

    private static final int LINE_X = 8;
    private static final int FIRST_LINE_Y = 18;
    private static final int LINE_HEIGHT = 10;

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderInfo(GuiGraphics graphics) {
        drawLine(graphics, 0, text("turbine_controller.steam", menu.getSteamBuffered(), menu.getSteamCapacity()));
        drawLine(graphics, 1, text("turbine_controller.steam_use", menu.getSteamUse()));
        drawLine(graphics, 2, text("turbine_controller.stages", menu.getStages()));
        drawLine(graphics, 3, text("turbine_controller.output", menu.getOutputPercent()));
        drawLine(graphics, 4, text("turbine_controller.speed", menu.getGeneratedRpm()));
    }

    private void drawLine(GuiGraphics graphics, int line, Component text) {
        graphics.drawString(this.font, text, LINE_X, FIRST_LINE_Y + line * LINE_HEIGHT, TEXT_COLOR, false);
    }
}
