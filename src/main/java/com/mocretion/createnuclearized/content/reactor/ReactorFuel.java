package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNFluids;

import net.minecraft.world.level.material.Fluid;

public enum ReactorFuel {

    URANIUM(4978, 50, 40, Element.URANIUM, Element.PLUTONIUM),
    PLUTONIUM(7467, 60, 8, Element.PLUTONIUM, Element.AMERICIUM),
    THORIUM_URANIUM(3111, 45, 55, Element.THORIUM, Element.URANIUM);

    public enum Element { URANIUM, PLUTONIUM, THORIUM, AMERICIUM }

    private final int maxHeat;
    private final int baseBurn;
    private final int basePurity;
    @Nullable
    private final Element unconsumed;
    @Nullable
    private final Element bred;

    ReactorFuel(int maxHeat, int baseBurn, int basePurity,
            @Nullable Element unconsumed, @Nullable Element bred) {
        this.maxHeat = maxHeat;
        this.baseBurn = baseBurn;
        this.basePurity = basePurity;
        this.unconsumed = unconsumed;
        this.bred = bred;
    }

    public int maxHeat() {
        return maxHeat;
    }

    public int baseBurn() {
        return baseBurn;
    }

    public int basePurity() {
        return basePurity;
    }

    @Nullable
    public Element unconsumed() {
        return unconsumed;
    }

    @Nullable
    public Element bred() {
        return bred;
    }

    public int heatFor(int burn) {
        return Math.round(maxHeat * clampPercent(burn) / 100f);
    }

    public SaltStats composition(int burn, int purity) {
        burn = clampPercent(burn);
        purity = clampPercent(purity);
        int unconsumedPct = 100 - burn;
        int bredPct = bred == null ? 0 : Math.round(burn * purity / 100f);
        int offGasPct = burn - bredPct;

        int[] elems = new int[Element.values().length];
        if (unconsumed != null)
            elems[unconsumed.ordinal()] += unconsumedPct;
        if (bred != null)
            elems[bred.ordinal()] += bredPct;
        return new SaltStats(heatFor(burn),
                elems[Element.URANIUM.ordinal()],
                elems[Element.PLUTONIUM.ordinal()],
                elems[Element.THORIUM.ordinal()],
                elems[Element.AMERICIUM.ordinal()],
                offGasPct);
    }

    public SaltStats baseComposition() {
        return composition(baseBurn, basePurity);
    }

    @Nullable
    public String role(Element element) {
        if (element == unconsumed) return "unconsumed";
        if (element == bred) return "bred";
        return null;
    }

    private static int clampPercent(int v) {
        return Math.max(0, Math.min(100, v));
    }

    public Fluid inputFluid() {
        return switch (this) {
            case URANIUM -> CNFluids.uraniumSalt();
            case PLUTONIUM -> CNFluids.plutoniumSalt();
            case THORIUM_URANIUM -> CNFluids.thoriumUraniumSalt();
        };
    }

    public Fluid outputFluid() {
        return switch (this) {
            case URANIUM -> CNFluids.spentUraniumSalt();
            case PLUTONIUM -> CNFluids.spentPlutoniumSalt();
            case THORIUM_URANIUM -> CNFluids.spentThoriumUraniumSalt();
        };
    }

    @Nullable
    public static ReactorFuel forOutput(Fluid fluid) {
        for (ReactorFuel fuel : values()) {
            if (fluid == fuel.outputFluid()) return fuel;
        }
        return null;
    }

    @Nullable
    public static ReactorFuel forInput(Fluid fluid) {
        for (ReactorFuel fuel : values()) {
            if (fluid == fuel.inputFluid()) return fuel;
        }
        return null;
    }

    public static boolean isInput(Fluid fluid) {
        return forInput(fluid) != null;
    }

    public static boolean isOutput(Fluid fluid) {
        return forOutput(fluid) != null;
    }
}
