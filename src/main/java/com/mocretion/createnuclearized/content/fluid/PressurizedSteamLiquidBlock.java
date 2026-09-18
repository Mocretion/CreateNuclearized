package com.mocretion.createnuclearized.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class PressurizedSteamLiquidBlock extends LiquidBlock {

    // A placed steam source removes itself after this many ticks (1 second)
    private static final int SOURCE_LIFETIME_TICKS = 20;

    public PressurizedSteamLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && state.getFluidState().isSource())
            level.scheduleTick(pos, this, SOURCE_LIFETIME_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Retire the source; rising steam drains on its own
        // Must be setBlock(AIR), not removeBlock: removeBlock re places getFluidState(pos).createLegacyBlock(), i.e. the same source block
        if (state.getFluidState().isSource())
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
