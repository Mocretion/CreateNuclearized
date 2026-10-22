package com.mocretion.createnuclearized.content.turbine;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.menu.ControllerScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TurbineControllerScreen extends ControllerScreen<TurbineControllerMenu> {

    private static final int LINE_X = 8;
    private static final int FIRST_LINE_Y = 22;
    private static final int LINE_HEIGHT = 10;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/turbine_controller.png");

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void renderInfo(GuiGraphics graphics) {
        drawLine(graphics, 0, text("turbine_controller.steam", menu.getSteamBuffered(), menu.getSteamCapacity()));
        drawLine(graphics, 1, text("turbine_controller.steam_use", menu.getSteamUse()));
        drawLine(graphics, 2, text("turbine_controller.stages", menu.getStages()));
        drawLine(graphics, 3, text("turbine_controller.output", menu.getOutputPercent()));
    }

    private void drawLine(GuiGraphics graphics, int line, Component text) {
        graphics.drawString(this.font, text, LINE_X, FIRST_LINE_Y + line * LINE_HEIGHT, TEXT_COLOR, false);
    }
}
