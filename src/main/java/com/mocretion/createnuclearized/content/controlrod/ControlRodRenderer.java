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

        // The block is non full but the interior is dim, so re sample light at the exposed neighbour above (like Create's pulley) instead
        // of trusting the near zero value handed in
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        int neighbourLight = level != null ? LevelRenderer.getLightColor(level, pos.above()) : light;

        Axis axis = Axis.Y;
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, pos, axis);

        // Spinning cogwheel (create textured placeholder, matching the reactor encased cogwheel); cutout so the tooth gaps read through the
        // casing side window
        SuperByteBuffer cog =
                CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state, Direction.UP);
        KineticBlockEntityRenderer.kineticRotationTransform(cog, be, axis, angle, neighbourLight);
        cog.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        // Rotating shaft nub on top, driven by the kinetic network (SHAFT_HALF already sits in the upper half poking out the top face, like
        // Create's encased cog, so no extra translate)
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.UP);
        KineticBlockEntityRenderer.kineticRotationTransform(shaft, be, axis, angle, neighbourLight);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));

        // Rod sliding down into the core by the current extension
        float length = be.getRenderedExtension(partialTicks);
        if (length > 1f / 16f) {
            SuperByteBuffer rod = CachedBuffers.partial(ControlRodPartials.CONTROL_ROD_EXTENSION, state);
            rod.translate(0, -length, 0);
            rod.scale(1, length, 1);
            rod.light(neighbourLight);
            rod.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ControlRodBlockEntity be) {
        // The rod hangs below the block; keep rendering it even when the anchor is off screen
        return true;
    }
}
