package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNFluids;

import net.minecraft.world.level.material.Fluid;

public final class ReactorFuel {

    private static final List<ReactorFuel> FUELS = new ArrayList<>();

    public static final ReactorFuel URANIUM = register(4978, 50, 40, SaltElement.URANIUM, SaltElement.PLUTONIUM,
            CNFluids::uraniumSalt, CNFluids::spentUraniumSalt);
    public static final ReactorFuel PLUTONIUM = register(7467, 60, 8, SaltElement.PLUTONIUM, SaltElement.AMERICIUM,
            CNFluids::plutoniumSalt, CNFluids::spentPlutoniumSalt);
    public static final ReactorFuel THORIUM_URANIUM = register(3111, 45, 55, SaltElement.THORIUM, SaltElement.URANIUM,
            CNFluids::thoriumUraniumSalt, CNFluids::spentThoriumUraniumSalt);

    private final int index;
    private int maxHeat;
    private int baseBurn;
    private int basePurity;
    @Nullable
    private SaltElement unconsumed;
    @Nullable
    private SaltElement bred;
    private final Supplier<Fluid> input;
    private boolean disabled;
    private final Supplier<Fluid> output;

    private ReactorFuel(int index, int maxHeat, int baseBurn, int basePurity, @Nullable SaltElement unconsumed,
            @Nullable SaltElement bred, Supplier<Fluid> input, Supplier<Fluid> output) {
        this.index = index;
        this.maxHeat = maxHeat;
        this.baseBurn = baseBurn;
        this.basePurity = basePurity;
        this.unconsumed = unconsumed;
        this.bred = bred;
        this.input = input;
        this.output = output;
    }

    public static synchronized ReactorFuel register(int maxHeat, int baseBurn, int basePurity,
            @Nullable SaltElement unconsumed, @Nullable SaltElement bred, Supplier<Fluid> input, Supplier<Fluid> output) {
        ReactorFuel fuel = new ReactorFuel(FUELS.size(), maxHeat, baseBurn, basePurity, unconsumed, bred, input, output);
        FUELS.add(fuel);
        return fuel;
    }

    public static List<ReactorFuel> values() {
        return Collections.unmodifiableList(FUELS);
    }

    public static ReactorFuel byIndex(int index) {
        return index >= 0 && index < FUELS.size() ? FUELS.get(index) : URANIUM;
    }

    public int index() {
        return index;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setMaxHeat(int maxHeat) {
        this.maxHeat = maxHeat;
    }

    public void setBaseBurn(int baseBurn) {
        this.baseBurn = baseBurn;
    }

    public void setBasePurity(int basePurity) {
        this.basePurity = basePurity;
    }

    public void setUnconsumed(@Nullable SaltElement unconsumed) {
        this.unconsumed = unconsumed;
    }

    public void setBred(@Nullable SaltElement bred) {
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
    public SaltElement unconsumed() {
        return unconsumed;
    }

    @Nullable
    public SaltElement bred() {
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

        SaltStats stats = new SaltStats(heatFor(burn), Map.of(), offGasPct);
        if (unconsumed != null)
            stats = stats.withElement(unconsumed, unconsumedPct);
        if (bred != null)
            stats = stats.withElement(bred, stats.amount(bred) + bredPct);
        return stats;
    }

    public SaltStats baseComposition() {
        return composition(baseBurn, basePurity);
    }

    @Nullable
    public String role(SaltElement element) {
        if (element == unconsumed) return "unconsumed";
        if (element == bred) return "bred";
        return null;
    }

    private static int clampPercent(int v) {
        return Math.max(0, Math.min(100, v));
    }

    public Fluid inputFluid() {
        return input.get();
    }

    public Fluid outputFluid() {
        return output.get();
    }

    @Nullable
    public static ReactorFuel forOutput(Fluid fluid) {
        for (ReactorFuel fuel : FUELS) {
            if (!fuel.disabled && fluid == fuel.outputFluid()) return fuel;
        }
        return null;
    }

    @Nullable
    public static ReactorFuel forInput(Fluid fluid) {
        for (ReactorFuel fuel : FUELS) {
            if (!fuel.disabled && fluid == fuel.inputFluid()) return fuel;
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
