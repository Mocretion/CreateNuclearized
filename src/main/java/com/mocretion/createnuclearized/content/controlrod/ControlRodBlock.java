package com.mocretion.createnuclearized.content.controlrod;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ControlRodBlock extends KineticBlock implements IBE<ControlRodBlockEntity>, ICogWheel {

    public ControlRodBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public Class<ControlRodBlockEntity> getBlockEntityClass() {
        return ControlRodBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ControlRodBlockEntity> getBlockEntityType() {
        return CNBlockEntities.CONTROL_ROD.get();
    }
}
