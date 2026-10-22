package com.mocretion.createnuclearized.content.reconstitution;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class Reconstitution {

    private Reconstitution() {}

    @Nullable
    public static ReconstitutionRecipe find(Level level, BasinBlockEntity basin) {
        for (RecipeHolder<ReconstitutionRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.RECONSTITUTION.get())) {
            if (canRun(holder.value(), basin)) return holder.value();
        }
        return null;
    }

    public static boolean canRun(ReconstitutionRecipe recipe, BasinBlockEntity basin) {
        IFluidHandler input = basin.getTanks().getFirst().getCapability();
        FluidStack salt = findSalt(input, recipe);
        if (salt.isEmpty()) return false;
        int needed = recipe.neededLiquid(salt.get(CNDataComponents.SALT_STATS.get()));
        return !findMetal(input, recipe, needed).isEmpty()
                && basin.acceptOutputs(List.of(), outputs(recipe), true);
    }

    public static void apply(ReconstitutionRecipe recipe, BasinBlockEntity basin) {
        IFluidHandler input = basin.getTanks().getFirst().getCapability();
        FluidStack salt = findSalt(input, recipe);
        if (salt.isEmpty()) return;
        int needed = recipe.neededLiquid(salt.get(CNDataComponents.SALT_STATS.get()));
        FluidStack metal = findMetal(input, recipe, needed);
        if (metal.isEmpty()) return;

        List<FluidStack> outputs = outputs(recipe);
        if (!basin.acceptOutputs(List.of(), outputs, true)) return;

        input.drain(salt.copyWithAmount(recipe.amount()), FluidAction.EXECUTE);
        input.drain(metal.copyWithAmount(needed), FluidAction.EXECUTE);
        basin.acceptOutputs(List.of(), outputs, false);
        basin.inputTank.sendDataImmediately();
        basin.notifyChangeOfContents();
    }

    private static List<FluidStack> outputs(ReconstitutionRecipe recipe) {
        List<FluidStack> list = new ArrayList<>();
        list.add(recipe.resultStack());
        return list;
    }

    private static FluidStack findSalt(IFluidHandler input, ReconstitutionRecipe recipe) {
        for (int tank = 0; tank < input.getTanks(); tank++) {
            FluidStack stack = input.getFluidInTank(tank);
            if (stack.getAmount() >= recipe.amount() && recipe.accepts(stack)) return stack;
        }
        return FluidStack.EMPTY;
    }

    private static FluidStack findMetal(IFluidHandler input, ReconstitutionRecipe recipe, int needed) {
        for (int tank = 0; tank < input.getTanks(); tank++) {
            FluidStack stack = input.getFluidInTank(tank);
            if (stack.getFluid() == recipe.liquid() && stack.getAmount() >= needed) return stack;
        }
        return FluidStack.EMPTY;
    }
}
