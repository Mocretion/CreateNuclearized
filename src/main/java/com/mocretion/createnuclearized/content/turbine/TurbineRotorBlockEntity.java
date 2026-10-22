package com.mocretion.createnuclearized.content.turbine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineRotorBlockEntity extends KineticBlockEntity {

    public TurbineRotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateAddedStressCapacity() {
        return 0; // registered capacity is for the tooltip only, the generator supplies the real SU
    }
}
