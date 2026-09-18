package com.mocretion.createnuclearized.content.boiler;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ContainedBlazeBurnerBlock extends Block implements EntityBlock {

    public ContainedBlazeBurnerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.CONTAINED_BLAZE_BURNER.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (!(be instanceof ContainedBlazeBurnerBlockEntity burner)) return;
            // Server drives the conversion, client drives the blaze head animation
            if (lvl.isClientSide)
                burner.clientTick();
            else
                burner.serverTick();
        };
    }
}
