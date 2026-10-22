package com.mocretion.createnuclearized.content.kinetics;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RtgBlock extends RotatedPillarKineticBlock implements IBE<RtgBlockEntity> {

    private final float generatedRpm;

    public RtgBlock(Properties properties, float generatedRpm) {
        super(properties);
        this.generatedRpm = generatedRpm;
    }

    public float getGeneratedRpm() {
        return generatedRpm;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<RtgBlockEntity> getBlockEntityClass() {
        return RtgBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RtgBlockEntity> getBlockEntityType() {
        return CNBlockEntities.RTG.get();
    }
}
