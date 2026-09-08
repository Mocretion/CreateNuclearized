package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.registry.CNBlocks;

import net.minecraft.world.level.block.state.BlockState;

public final class ReactorModerator {

    public static final double GRAPHITE_RATE = 0.05;
    public static final double BERYLLIUM_RATE = 0.1;

    private ReactorModerator() {}

    /** mB/tick a bridging instance of this block adds, or 0 if it isnt a moderator */
    public static double rate(BlockState state) {
        if (state.is(CNBlocks.GRAPHITE_BLOCK.get())) return GRAPHITE_RATE;
        if (state.is(CNBlocks.BERYLLIUM_BLOCK.get())) return BERYLLIUM_RATE;
        return 0;
    }

    public static boolean isModerator(BlockState state) {
        return rate(state) > 0;
    }
}
