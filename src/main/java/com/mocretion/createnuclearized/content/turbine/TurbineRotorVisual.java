package com.mocretion.createnuclearized.content.turbine;

import java.util.function.Consumer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class TurbineRotorVisual extends KineticBlockEntityVisual<TurbineRotorBlockEntity> implements SimpleTickableVisual {

    private final RotatingInstance rotor;
    private final RotatingInstance casing;

    public TurbineRotorVisual(VisualizationContext context, TurbineRotorBlockEntity be, float partialTick) {
        super(context, be, partialTick);
        int radius = be.getBlockState().getBlock() instanceof TurbineRotorBlock r ? r.radius() : 1;
        Axis axis = rotationAxis();

        rotor = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(TurbineRotorPartials.forRadius(radius)))
                .createInstance()
                .rotateToFace(Direction.UP, axis)
                .setup(be)
                .setPosition(getVisualPosition());
        rotor.setChanged();

        // Same base orientation as the disc, but left at speed 0 (and no rotation offset) so it never turns
        casing = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(TurbineRotorPartials.forRadiusStatic(radius)))
                .createInstance()
                .rotateToFace(Direction.UP, axis)
                .setRotationAxis(axis)
                .setPosition(getVisualPosition());
        casing.setChanged();
    }

    @Override
    public void update(float pt) {
        // Re run setup so the disc picks up speed changes; the casing never needs it
        rotor.setup(blockEntity).setChanged();
    }

    @Override
    public void tick(Context context) {
        applyOverstressEffect(blockEntity, rotor);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(rotor, casing);
    }

    @Override
    protected void _delete() {
        rotor.delete();
        casing.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotor);
        consumer.accept(casing);
    }
}
