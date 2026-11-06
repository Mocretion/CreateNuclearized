package com.mocretion.createnuclearized.content.meteringvalve;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

public class MeteringValveRenderer extends SafeBlockEntityRenderer<MeteringValveBlockEntity> {

    private static final float GLASS_INSET = 1 / 16f;
    private static final float PLATE_INSET = 4 / 16f;

    public MeteringValveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(MeteringValveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        TankSegment tank = be.getBehaviour(SmartFluidTankBehaviour.TYPE).getPrimaryTank();
        if (tank.isEmpty(partialTicks)) return;

        FluidStack fluid = tank.getRenderedFluid();
        float fill = Mth.clamp(tank.getTotalUnits(partialTicks) / MeteringValveBlockEntity.CAPACITY, 0, 1);
        Axis axis = be.getBlockState().getValue(MeteringValveBlock.FACING).getAxis();
        float xMin = inset(Axis.X, axis);
        float yMin = inset(Axis.Y, axis);
        float zMin = inset(Axis.Z, axis);
        float height = (1 - 2 * yMin) * fill;
        float bottom = fluid.getFluid().getFluidType().isLighterThanAir() ? 1 - yMin - height : yMin;

        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, xMin, bottom, zMin, 1 - xMin, bottom + height, 1 - zMin,
                buffer, ms, light, false, true);
    }

    private static float inset(Axis side, Axis valve) {
        return side == valve ? PLATE_INSET : GLASS_INSET;
    }
}
