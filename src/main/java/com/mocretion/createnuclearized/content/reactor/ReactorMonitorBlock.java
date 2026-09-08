package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ReactorMonitorBlock extends Block implements EntityBlock {

    public ReactorMonitorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ReactorMonitorBlockEntity be)
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.REACTOR_MONITOR.create(pos, state);
    }
}
