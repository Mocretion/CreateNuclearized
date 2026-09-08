package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorFluidValveBlock extends Block implements EntityBlock {

    private final boolean input;

    public ReactorFluidValveBlock(Properties properties, boolean input) {
        super(properties);
        this.input = input;
    }

    public boolean isInput() {
        return input;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.REACTOR_FLUID_VALVE.create(pos, state);
    }
}
