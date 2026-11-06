package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

public final class ReactorModifier {

    public record Effect(int dBurn, int dPurity, double dRate, int dHeat) {}

    private ReactorModifier() {}

    @Nullable
    public static Effect effect(BlockState state) {
        return ReactorComponents.coolerEffect(state.getBlock());
    }
}
