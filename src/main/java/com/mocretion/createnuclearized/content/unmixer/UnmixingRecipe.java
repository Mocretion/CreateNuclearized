package com.mocretion.createnuclearized.content.unmixer;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.content.boiler.FluidRecipeInput;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public record UnmixingRecipe(FluidIngredient input, int amount, List<FluidStack> fluidResults, List<ItemStack> itemResults)
        implements Recipe<FluidRecipeInput> {

    public static final MapCodec<UnmixingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(UnmixingRecipe::input),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("amount", 1000).forGetter(UnmixingRecipe::amount),
            FluidStack.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(UnmixingRecipe::fluidResults),
            ItemStack.CODEC.listOf().optionalFieldOf("item_results", List.of()).forGetter(UnmixingRecipe::itemResults)
    ).apply(i, UnmixingRecipe::new));

    public boolean isDynamic() {
        return fluidResults.isEmpty() && itemResults.isEmpty();
    }

    public boolean accepts(FluidStack stack) {
        if (!input.test(stack)) return false;
        if (!isDynamic()) return true;
        ReactorFuel fuel = ReactorFuel.forOutput(stack.getFluid());
        if (fuel == null) return false;
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats == null) return false;
        return unconsumedPct(fuel, stats) > 0 || bredPct(fuel, stats) > 0;
    }

    public List<FluidStack> fluidOutputsFor(FluidStack in) {
        List<FluidStack> outs = new ArrayList<>();
        if (!isDynamic()) {
            for (FluidStack out : fluidResults)
                outs.add(out.copy());
            return outs;
        }

        ReactorFuel fuel = ReactorFuel.forOutput(in.getFluid());
        if (fuel == null) return outs;
        SaltStats stats = in.getOrDefault(CNDataComponents.SALT_STATS.get(), SaltStats.EMPTY);

        ReactorFuel.Element element = target(fuel, stats);
        if (element == null) return outs;

        outs.add(CNFluids.spentOrMolten(in.getFluid(), stats.withElement(element, 0), amount));
        outs.add(new FluidStack(CNFluids.liquidFor(element).builtInRegistryHolder(), stats.amount(element) * amount / 100));
        return outs;
    }

    public List<ItemStack> itemOutputsFor(FluidStack in) {
        if (isDynamic()) return List.of();
        List<ItemStack> outs = new ArrayList<>();
        for (ItemStack out : itemResults)
            outs.add(out.copy());
        return outs;
    }

    private static ReactorFuel.Element target(ReactorFuel fuel, SaltStats stats) {
        if (bredPct(fuel, stats) > 0) return fuel.bred();
        if (unconsumedPct(fuel, stats) > 0) return fuel.unconsumed();
        return null;
    }

    private static int unconsumedPct(ReactorFuel fuel, SaltStats stats) {
        return fuel.unconsumed() == null ? 0 : stats.amount(fuel.unconsumed());
    }

    private static int bredPct(ReactorFuel fuel, SaltStats stats) {
        return fuel.bred() == null ? 0 : stats.amount(fuel.bred());
    }

    @Override
    public boolean matches(FluidRecipeInput in, Level level) {
        return accepts(in.fluid());
    }

    @Override
    public ItemStack assemble(FluidRecipeInput in, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CNRecipeTypes.UNMIXING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CNRecipeTypes.UNMIXING.get();
    }
}
