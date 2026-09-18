package com.mocretion.createnuclearized.content.boiler;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ExhaustHoodBlock extends Block implements EntityBlock {

    public ExhaustHoodBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.EXHAUST_HOOD.create(pos, state);
    }
}
