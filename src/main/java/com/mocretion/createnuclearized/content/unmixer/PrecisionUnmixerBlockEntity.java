package com.mocretion.createnuclearized.content.unmixer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class PrecisionUnmixerBlockEntity extends MechanicalMixerBlockEntity {

    private static final Object unmixingRecipesKey = new Object();

    public PrecisionUnmixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<Recipe<?>> getMatchingRecipes() {
        List<Recipe<?>> list = new ArrayList<>();
        if (level == null) return list;
        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return list;
        BasinBlockEntity basin = basinOpt.get();

        for (RecipeHolder<UnmixingRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.UNMIXING.get())) {
            UnmixingRecipe recipe = holder.value();
            FluidStack in = findInput(basin, recipe);
            if (in.isEmpty()) continue;
            if (basin.acceptOutputs(recipe.itemOutputsFor(in), recipe.fluidOutputsFor(in), true))
                list.add(recipe);
        }
        return list;
    }

    @Override
    protected void applyBasinRecipe() {
        if (!(currentRecipe instanceof UnmixingRecipe recipe) || level == null) return;
        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return;
        BasinBlockEntity basin = basinOpt.get();

        FluidStack in = findInput(basin, recipe);
        if (in.isEmpty()) return;
        List<FluidStack> fluidOutputs = recipe.fluidOutputsFor(in);
        List<ItemStack> itemOutputs = recipe.itemOutputsFor(in);
        boolean wasEmpty = basin.canContinueProcessing();
        if (!basin.acceptOutputs(itemOutputs, fluidOutputs, true)) return;

        in.shrink(recipe.amount());
        basin.getTanks().getFirst().forEach(TankSegment::onFluidStackChanged);
        basin.acceptOutputs(itemOutputs, fluidOutputs, false);
        basin.inputTank.sendDataImmediately();

        if (wasEmpty) {
            FluidStack next = findInput(basin, recipe);
            if (!next.isEmpty() && basin.acceptOutputs(recipe.itemOutputsFor(next), recipe.fluidOutputsFor(next), true)) {
                continueWithPreviousRecipe();
                sendData();
            }
        }
        basin.notifyChangeOfContents();
    }

    private FluidStack findInput(BasinBlockEntity basin, UnmixingRecipe recipe) {
        IFluidHandler input = basin.getTanks().getFirst().getCapability();
        for (int tank = 0; tank < input.getTanks(); tank++) {
            FluidStack stack = input.getFluidInTank(tank);
            if (stack.getAmount() >= recipe.amount() && recipe.accepts(stack))
                return stack;
        }
        return FluidStack.EMPTY;
    }

    @Override
    protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
        return recipe.value().getType() == CNRecipeTypes.UNMIXING.get();
    }

    @Override
    protected Object getRecipeCacheKey() {
        return unmixingRecipesKey;
    }

    @Override
    protected Optional<CreateAdvancement> getProcessedRecipeTrigger() {
        return Optional.empty();
    }
}
