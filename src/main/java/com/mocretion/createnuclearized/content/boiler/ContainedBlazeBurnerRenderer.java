package com.mocretion.createnuclearized.content.boiler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ContainedBlazeBurnerRenderer extends SafeBlockEntityRenderer<ContainedBlazeBurnerBlockEntity> {

    public ContainedBlazeBurnerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(ContainedBlazeBurnerBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource bufferSource, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();

        // Reuse Create's blaze rendering, passing our helmet as the hat
        // Head bobs (the sine offset lives in renderShared) and, via the block entity, peeks up + faces forward while working, otherwise
        // sinks and watches the player. Small blaze when idle (SMOULDERING), big peeking blaze with a flame when converting (KINDLED)
        boolean active = be.isActive();
        float animation = be.getHeadAnimation(partialTicks) * 0.175f;
        float horizontalAngle = AngleHelper.rad(be.getHeadAngle(partialTicks));
        HeatLevel heat = active ? HeatLevel.KINDLED : HeatLevel.SMOULDERING;
        BlazeBurnerRenderer.renderShared(ms, null, bufferSource,
                level, state, heat, animation, horizontalAngle,
                active, false, ContainedBlazeBurnerPartials.HAZMAT_HELMET, be.hashCode());
    }
}
