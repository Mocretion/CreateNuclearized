package com.mocretion.createnuclearized.content.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperBufferFactory;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineRotorRenderer extends KineticBlockEntityRenderer<TurbineRotorBlockEntity> {

    private static final SuperByteBufferCache.Compartment<RotorKey> ROTOR = new SuperByteBufferCache.Compartment<>();
    private static final SuperByteBufferCache.Compartment<RotorKey> CASING = new SuperByteBufferCache.Compartment<>();

    public TurbineRotorRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(TurbineRotorBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;
        BlockState state = be.getBlockState();
        getCasingModel(be, state).light(light).renderInto(ms, buffer.getBuffer(getRenderType(be, state)));
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(TurbineRotorBlockEntity be, BlockState state) {
        int radius = state.getBlock() instanceof TurbineRotorBlock rotor ? rotor.radius() : 1;
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        return SuperByteBufferCache.getInstance().get(ROTOR, new RotorKey(radius, axis),
                () -> buildOriented(TurbineRotorPartials.forRadius(radius).get(), axis));
    }

    private SuperByteBuffer getCasingModel(TurbineRotorBlockEntity be, BlockState state) {
        int radius = state.getBlock() instanceof TurbineRotorBlock rotor ? rotor.radius() : 1;
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        return SuperByteBufferCache.getInstance().get(CASING, new RotorKey(radius, axis),
                () -> buildOriented(TurbineRotorPartials.forRadiusStatic(radius).get(), axis));
    }

    private static SuperByteBuffer buildOriented(BakedModel model, Axis axis) {
        Direction dir = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
        PoseStack transform = CachedBuffers.rotateToFaceVertical(dir).get();
        return SuperBufferFactory.getInstance().createForBlock(model, Blocks.AIR.defaultBlockState(), transform);
    }

    private record RotorKey(int radius, Axis axis) {}
}
