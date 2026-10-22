package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.registry.CNBlocks;

import net.minecraft.world.level.block.state.BlockState;

public final class ReactorReflector {

    public static final int BERYLLIUM_PERCENT = 1;

    private ReactorReflector() {}

    public static int percent(BlockState state) {
        if (state.is(CNBlocks.BERYLLIUM_BLOCK.get())) return BERYLLIUM_PERCENT;
        return 0;
    }
}
