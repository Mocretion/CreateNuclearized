package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.registry.CNBlocks;

import net.minecraft.world.level.block.state.BlockState;

public final class ReactorModerator {

    public static final double GRAPHITE_RATE = 0.05;

    private ReactorModerator() {}

    public static double rate(BlockState state) {
        if (state.is(CNBlocks.GRAPHITE_BLOCK.get())) return GRAPHITE_RATE;
        return 0;
    }
}
