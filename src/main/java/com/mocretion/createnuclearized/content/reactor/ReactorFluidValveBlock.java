package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorFluidValveBlock extends Block implements IBE<ReactorFluidValveBlockEntity> {

    private final boolean input;

    public ReactorFluidValveBlock(Properties properties, boolean input) {
        super(properties);
        this.input = input;
    }

    public boolean isInput() {
        return input;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<ReactorFluidValveBlockEntity> getBlockEntityClass() {
        return ReactorFluidValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactorFluidValveBlockEntity> getBlockEntityType() {
        return CNBlockEntities.REACTOR_FLUID_VALVE.get();
    }
}
