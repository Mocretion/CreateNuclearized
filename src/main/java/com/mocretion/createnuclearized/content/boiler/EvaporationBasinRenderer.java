package com.mocretion.createnuclearized.content.boiler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class EvaporationBasinRenderer extends SafeBlockEntityRenderer<EvaporationBasinBlockEntity> {

    public EvaporationBasinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(EvaporationBasinBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
        TankSegment[] tanks = {
                be.getBehaviour(SmartFluidTankBehaviour.INPUT).getPrimaryTank(),
                be.getBehaviour(SmartFluidTankBehaviour.OUTPUT).getPrimaryTank() };
        float total = units(tanks[0], partialTicks) + units(tanks[1], partialTicks);
        if (total < 1) return;

        float yMin = 10 / 16f;
        float yMax = 11 / 16f;

        float xMin = 2 / 16f;
        for (TankSegment tank : tanks) {
            if (tank.isEmpty(partialTicks)) continue;
            float xMax = xMin + tank.getTotalUnits(partialTicks) / total * 12 / 16f;
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(tank.getRenderedFluid(), xMin, yMin, 2 / 16f, xMax, yMax, 14 / 16f,
                    buffer, ms, light, false, false);
            xMin = xMax;
        }
    }

    private static float units(TankSegment tank, float partialTicks) {
        return tank.isEmpty(partialTicks) ? 0 : tank.getTotalUnits(partialTicks);
    }
}
