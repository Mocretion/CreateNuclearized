package com.mocretion.createnuclearized.content.kinetics;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RtgBlockEntity extends GeneratingKineticBlockEntity {

    public RtgBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || Math.abs(getGeneratedSpeed()) > Math.abs(getTheoreticalSpeed()))
            updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!(getBlockState().getBlock() instanceof RtgBlock rtg))
            return 0;
        float rpm = rtg.getGeneratedRpm();
        float current = getTheoreticalSpeed();
        return current == 0 ? rpm : Math.copySign(rpm, current);
    }
}
