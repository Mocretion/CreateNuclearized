package com.mocretion.createnuclearized.compat.jei;

import com.mocretion.createnuclearized.content.boiler.IndustrialHeaterBlock;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.client.gui.GuiGraphics;

public class AnimatedIndustrialHeater extends AnimatedKinetics {

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 200);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = 23;

        blockElement(CNBlocks.INDUSTRIAL_HEATER.getDefaultState()
                .setValue(IndustrialHeaterBlock.HEAT_LEVEL, HeatLevel.KINDLED)
                .setValue(IndustrialHeaterBlock.TEMPERATURE, IndustrialHeaterBlock.Temperature.MID))
                .atLocal(0, 1.65, 0)
                .scale(scale)
                .render(graphics);

        matrixStack.popPose();
    }
}
