package com.mocretion.createnuclearized.content.reactor;

import net.minecraft.world.level.block.state.BlockState;

public final class ReactorModerator {

    private ReactorModerator() {}

    public static double rate(BlockState state) {
        return ReactorComponents.moderatorRate(state.getBlock());
    }

    public static int heat(BlockState state) {
        return ReactorComponents.moderatorHeat(state.getBlock());
    }
}
