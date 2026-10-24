package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.menu.ControllerScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ReactorControllerScreen extends ControllerScreen<ReactorControllerMenu> {

    private static final int LINE_X = 8;
    private static final int FIRST_LINE_Y = 24;
    private static final int LINE_HEIGHT = 12;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/reactor_controller.png");

    public ReactorControllerScreen(ReactorControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new DrainToggleButton(texture(), this.leftPos, this.topPos, this.menu));
    }

    @Override
    protected void renderInfo(GuiGraphics graphics) {
        drawLine(graphics, 0, text("reactor_controller.fissile_capacity", menu.getFissileCapacity()));
        drawLine(graphics, 1, text("reactor_controller.spent_capacity", menu.getSpentCapacity()));
        drawLine(graphics, 2, text("reactor_controller.channels", menu.getTotalChannels(), drainLabel()));
    }

    private Component drainLabel() {
        int spent = menu.getSpentDrainCount();
        int fuel = menu.getFuelDrainCount();
        String key = spent > 0 && fuel > 0 ? "mixed" : fuel > 0 ? "fuel" : "spent";
        return text("reactor_controller.drain." + key);
    }

    private void drawLine(GuiGraphics graphics, int line, Component text) {
        graphics.drawString(this.font, text, LINE_X, FIRST_LINE_Y + line * LINE_HEIGHT, TEXT_COLOR, false);
    }
}
