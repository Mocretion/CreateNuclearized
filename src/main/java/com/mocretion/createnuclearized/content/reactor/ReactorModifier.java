package com.mocretion.createnuclearized.content.reactor;

import java.util.Map;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlocks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorModifier {

    public record Effect(int dBurn, int dPurity, double dRate) {}

    public static final Effect AIR = new Effect(0, 0, 0.005);
    public static final Effect WATER_SOURCE = new Effect(0, 0, 0.0075);
    public static final Effect XENON = new Effect(0, 0, 0.05);

    private static final Map<net.minecraft.world.level.block.Block, Effect> BY_BLOCK = Map.of(
            Blocks.SCULK, new Effect(-10, 0, 0),
            Blocks.SEA_LANTERN, new Effect(10, 0, 0),
            Blocks.GLOWSTONE, new Effect(5, 0, 0),
            Blocks.SPONGE, new Effect(0, 15, 0),
            Blocks.AMETHYST_BLOCK, new Effect(0, 5, 0),
            Blocks.HONEY_BLOCK, new Effect(0, -15, 0),
            Blocks.DIAMOND_BLOCK, new Effect(0, 0, 0.03));

    private ReactorModifier() {}

    @Nullable
    public static Effect effect(BlockState state) {
        if (state.isAir()) return AIR;
        if (state.is(Blocks.WATER)) return state.getFluidState().isSource() ? WATER_SOURCE : null;
        if (state.is(CNBlocks.XENON_BLOCK.get())) return XENON;
        return BY_BLOCK.get(state.getBlock());
    }

    public static boolean isModifier(BlockState state) {
        return effect(state) != null;
    }
}
