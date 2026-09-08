package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNFluids;

import net.minecraft.world.level.material.Fluid;

public enum ReactorFuel {

    URANIUM(400, 50, 40, Element.URANIUM, Element.PLUTONIUM),
    PLUTONIUM(600, 60, 0, Element.PLUTONIUM, null),
    THORIUM_URANIUM(250, 45, 55, Element.THORIUM, Element.URANIUM);

    public enum Element { URANIUM, PLUTONIUM, THORIUM }

    // Heat at 100% burn; real heat scales with burn (heatFor)
    private final int maxHeat;
    private final int baseBurn;
    private final int basePurity;
    @Nullable
    private final Element leftover;
    @Nullable
    private final Element generated;

    ReactorFuel(int maxHeat, int baseBurn, int basePurity,
            @Nullable Element leftover, @Nullable Element generated) {
        this.maxHeat = maxHeat;
        this.baseBurn = baseBurn;
        this.basePurity = basePurity;
        this.leftover = leftover;
        this.generated = generated;
    }

    public int maxHeat() {
        return maxHeat;
    }

    /** Baseline burn % before any cooler */
    public int baseBurn() {
        return baseBurn;
    }

    /** Baseline purity % before any cooler */
    public int basePurity() {
        return basePurity;
    }

    /** The unspent fuel element for this fuel */
    @Nullable
    public Element leftover() {
        return leftover;
    }

    /** The element bred during the reaction, or null if this fuel generates nothing */
    @Nullable
    public Element generated() {
        return generated;
    }

    /** Heat at a given burn %, maxHeat scaled linearly */
    public int heatFor(int burn) {
        return Math.round(maxHeat * clampPercent(burn) / 100f);
    }

    /**
     * The heated salt composition for a burn/purity: leftover, generated, and byproduct percentages (they sum to 100) mapped onto this
     * fuels elements, plus heat from burn
     */
    public SaltStats composition(int burn, int purity) {
        burn = clampPercent(burn);
        purity = clampPercent(purity);
        int leftoverPct = 100 - burn;
        int generatedPct = generated == null ? 0 : Math.round(burn * purity / 100f);
        int byproductPct = burn - generatedPct;

        int[] elems = new int[Element.values().length]; // indexed by Element.ordinal()
        if (leftover != null)
            elems[leftover.ordinal()] += leftoverPct;
        if (generated != null)
            elems[generated.ordinal()] += generatedPct;
        return new SaltStats(heatFor(burn),
                elems[Element.URANIUM.ordinal()],
                elems[Element.PLUTONIUM.ordinal()],
                elems[Element.THORIUM.ordinal()],
                byproductPct);
    }

    /** The composition this fuel makes with no coolers applied */
    public SaltStats baseComposition() {
        return composition(baseBurn, basePurity);
    }

    /** The role a composition element plays for this fuel, or null if neither */
    public String role(Element element) {
        if (element == leftover) return "leftover";
        if (element == generated) return "generated";
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
            case URANIUM -> CNFluids.depletedUraniumSalt();
            case PLUTONIUM -> CNFluids.depletedPlutoniumSalt();
            case THORIUM_URANIUM -> CNFluids.depletedThoriumUraniumSalt();
        };
    }

    /** The fuel whose depleted output is this fluid, or null if it is not a reactor output */
    public static ReactorFuel forOutput(Fluid fluid) {
        for (ReactorFuel fuel : values()) {
            if (fluid == fuel.outputFluid()) return fuel;
        }
        return null;
    }

    /** The fuel a given input salt belongs to, defaulting to uranium for anything else */
    public static ReactorFuel forInput(Fluid fluid) {
        if (fluid == CNFluids.plutoniumSalt()) return PLUTONIUM;
        if (fluid == CNFluids.thoriumUraniumSalt()) return THORIUM_URANIUM;
        return URANIUM;
    }

    /** True if the fluid is any fuel input salt */
    public static boolean isInput(Fluid fluid) {
        return fluid == CNFluids.uraniumSalt() || fluid == CNFluids.plutoniumSalt()
                || fluid == CNFluids.thoriumUraniumSalt();
    }

    /** True if the fluid is any depleted output salt */
    public static boolean isOutput(Fluid fluid) {
        return fluid == CNFluids.depletedUraniumSalt() || fluid == CNFluids.depletedPlutoniumSalt()
                || fluid == CNFluids.depletedThoriumUraniumSalt();
    }
}
