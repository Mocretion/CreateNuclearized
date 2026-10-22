package com.mocretion.createnuclearized.content.controlrod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ControlRodRenderer extends KineticBlockEntityRenderer<ControlRodBlockEntity> {

    public ControlRodRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ControlRodBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();

        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        int neighborLight = level != null ? LevelRenderer.getLightColor(level, pos.above()) : light;

        Axis axis = Axis.Y;
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, pos, axis);

        SuperByteBuffer cog =
                CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state, Direction.UP);
        KineticBlockEntityRenderer.kineticRotationTransform(cog, be, axis, angle, neighborLight);
        cog.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.UP);
        KineticBlockEntityRenderer.kineticRotationTransform(shaft, be, axis, angle, neighborLight);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));

        float length = be.getRenderedExtension(partialTicks);
        if (length <= 1f / 16f) return;

        int fullSegments = (int) length;
        for (int i = 0; i < fullSegments; i++)
            renderSegment(state, ms, buffer, neighborLight, i + 1f, 1f);

        float remainder = length - fullSegments;
        if (remainder > 1f / 16f)
            renderSegment(state, ms, buffer, neighborLight, length, remainder);
    }

    private static void renderSegment(BlockState state, PoseStack ms, MultiBufferSource buffer,
                                      int light, float bottom, float height) {
        SuperByteBuffer rod = CachedBuffers.partial(ControlRodPartials.CONTROL_ROD_EXTENSION, state);
        rod.translate(0, -bottom, 0);
        rod.scale(1, height, 1);
        rod.light(light);
        rod.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    public boolean shouldRenderOffScreen(ControlRodBlockEntity be) {
        return true;
    }
}
