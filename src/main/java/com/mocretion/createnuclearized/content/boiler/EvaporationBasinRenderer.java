package com.mocretion.createnuclearized.content.boiler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class EvaporationBasinRenderer extends SafeBlockEntityRenderer<EvaporationBasinBlockEntity> {

    private static final float WALL = 2 / 16f;

    public EvaporationBasinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(EvaporationBasinBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        renderFilter(be, ms, buffer, light, overlay);
        if (!be.isController()) return;
        TankSegment[] tanks = {
                be.getBehaviour(SmartFluidTankBehaviour.INPUT).getPrimaryTank(),
                be.getBehaviour(SmartFluidTankBehaviour.OUTPUT).getPrimaryTank() };
        float total = units(tanks[0], partialTicks) + units(tanks[1], partialTicks);
        if (total < 1) return;

        int fluidLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        float yMin = 10 / 16f;
        float yMax = 11 / 16f;
        float inner = be.getWidth() - 2 * WALL;
        float far = be.getWidth() - WALL;

        float xMin = WALL;
        for (TankSegment tank : tanks) {
            if (tank.isEmpty(partialTicks)) continue;
            float xMax = xMin + tank.getTotalUnits(partialTicks) / total * inner;
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(tank.getRenderedFluid(), xMin, yMin, WALL, xMax, yMax, far,
                    buffer, ms, fluidLight, false, false);
            xMin = xMax;
        }
    }

    private static void renderFilter(EvaporationBasinBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ItemStack filter = be.getFiltering().getFilter();
        if (filter.isEmpty()) return;
        EvaporationBasinBlockEntity.BasinValueBox box = be.getValueBox();
        Direction previous = box.getSide();
        for (Direction side : Iterate.horizontalDirections) {
            box.fromSide(side);
            if (!box.drawsItem(be.getLevel(), be.getBlockPos(), be.getBlockState())) continue;
            ms.pushPose();
            box.transform(be.getLevel(), be.getBlockPos(), be.getBlockState(), ms);
            ValueBoxRenderer.renderItemIntoValueBox(filter, ms, buffer, light, overlay);
            ms.popPose();
        }
        box.fromSide(previous);
    }

    private static float units(TankSegment tank, float partialTicks) {
        return tank.isEmpty(partialTicks) ? 0 : tank.getTotalUnits(partialTicks);
    }
}
