package com.mocretion.createnuclearized.content.turbine;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineGeneratorBlock extends RotatedPillarKineticBlock implements IBE<TurbineGeneratorBlockEntity> {

    public TurbineGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<TurbineGeneratorBlockEntity> getBlockEntityClass() {
        return TurbineGeneratorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineGeneratorBlockEntity> getBlockEntityType() {
        return CNBlockEntities.TURBINE_GENERATOR.get();
    }
}
