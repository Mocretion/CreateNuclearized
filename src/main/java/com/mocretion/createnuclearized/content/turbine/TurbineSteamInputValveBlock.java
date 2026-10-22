package com.mocretion.createnuclearized.content.turbine;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.blockentity.SyncedBlockEntity;
import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineSteamInputValveBlock extends Block implements EntityBlock {

    public TurbineSteamInputValveBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.TURBINE_STEAM_INPUT_VALVE.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        return SyncedBlockEntity.serverTicker(level);
    }
}
