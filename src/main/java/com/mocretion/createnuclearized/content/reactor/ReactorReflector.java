package com.mocretion.createnuclearized.content.reactor;

import net.minecraft.world.level.block.state.BlockState;

public final class ReactorReflector {

    private ReactorReflector() {}

    public static int heat(BlockState state) {
        return ReactorComponents.reflectorHeat(state.getBlock());
    }
}
