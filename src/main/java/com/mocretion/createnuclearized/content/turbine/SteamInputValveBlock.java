package com.mocretion.createnuclearized.content.turbine;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SteamInputValveBlock extends Block implements EntityBlock {

    public SteamInputValveBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.STEAM_INPUT_VALVE.create(pos, state);
    }
}
