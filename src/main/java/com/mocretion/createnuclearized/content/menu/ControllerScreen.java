package com.mocretion.createnuclearized.content.menu;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.multiblock.MultiblockState;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class ControllerScreen<M extends ControllerMenu> extends AbstractContainerScreen<M> {

    protected static final int TEXT_COLOR = 0x404040;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CreateNuclearized.MODID, "textures/gui/reactor_controller.png");

    protected ControllerScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        addRenderableWidget(new StartStopButton(TEXTURE, this.leftPos, this.topPos, this.menu));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        MultiblockState state = menu.getState();
        graphics.drawString(this.font, state.label(), this.titleLabelX, this.titleLabelY, state.color(), false);
        renderInfo(graphics);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_COLOR, false);
    }

    protected abstract void renderInfo(GuiGraphics graphics);

    protected static Component text(String key, Object... args) {
        return Component.translatable(CreateNuclearized.MODID + ".gui." + key, args);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
