package com.mocretion.createnuclearized.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.content.boiler.HeatGenerationRecipe;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import java.util.Optional;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class HeatGenerationCategory extends CNRecipeCategory<HeatGenerationRecipe> {

    private static final int DISPLAY = 100;
    private static final int HEATED_COLOR = 0xFFE88300;
    private static final int BAR_Y = 48;
    private static final int SCENE_RAISE = 13;
    private static final int BURNER_Y = BAR_Y - 25 - SCENE_RAISE;
    private static final int LIGHT_Y = BAR_Y + 8 - SCENE_RAISE;

    private final AnimatedBlazeBurner burner = new AnimatedBlazeBurner();

    public HeatGenerationCategory(Info<HeatGenerationRecipe> info, IGuiHelper guiHelper) {
        super(info, guiHelper);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HeatGenerationRecipe recipe, IFocusGroup focuses) {
        List<FluidStack> inputs = inputExamples(recipe);
        IRecipeSlotBuilder inputSlot = fluidSlot(builder, RecipeIngredientRole.INPUT, 36, 30, inputs);
        hideFluidAmount(inputSlot);
        saltTooltip(inputSlot);

        List<FluidStack> outputs = outputExamples(recipe, inputs);
        if (!outputs.isEmpty()) {
            IRecipeSlotBuilder outputSlot = fluidSlot(builder, RecipeIngredientRole.OUTPUT, 142, 30, outputs);
            hideFluidAmount(outputSlot);
            saltTooltip(outputSlot);
        }
    }

    @Override
    public void draw(HeatGenerationRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (hasLiquidOutput(recipe))
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 12);

        int sceneX = getBackground().getWidth() / 2 + 3;
        AllGuiTextures.JEI_LIGHT.render(graphics, sceneX - 10, LIGHT_Y);
        renderMirroredY(graphics, AllGuiTextures.JEI_HEAT_BAR, 4, BAR_Y);
        line(graphics, Component.translatable("createnuclearized.jei.heat_generated", displayedHeat(recipe, slots)),
                9, BAR_Y + 6, HEATED_COLOR, 0.95f);
        burner.withHeat(HeatLevel.KINDLED).draw(graphics, sceneX, BURNER_Y);
    }

    private static boolean hasLiquidOutput(HeatGenerationRecipe recipe) {
        return !outputExamples(recipe, inputExamples(recipe)).isEmpty();
    }

    private static int displayedHeat(HeatGenerationRecipe recipe, IRecipeSlotsView slots) {
        if (!recipe.usesFluidHeat()) return recipe.heat().orElse(0);
        for (IRecipeSlotView view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            Optional<FluidStack> shown = view.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK);
            if (shown.isPresent()) return recipe.heatFor(shown.get());
        }
        for (FluidStack input : inputExamples(recipe))
            return recipe.heatFor(input);
        return 0;
    }

    private static List<FluidStack> inputExamples(HeatGenerationRecipe recipe) {
        List<FluidStack> out = new ArrayList<>();
        for (FluidStack stack : recipe.input().getStacks()) {
            if (!stack.getFluid().defaultFluidState().isSource()) continue;
            out.add(heaterDisplay(stack.getFluid()));
        }
        return out;
    }

    private static List<FluidStack> outputExamples(HeatGenerationRecipe recipe, List<FluidStack> inputs) {
        List<FluidStack> out = new ArrayList<>();
        for (FluidStack input : inputs) {
            FluidStack result = recipe.output(input, DISPLAY);
            if (!result.isEmpty()) out.add(result);
        }
        return out;
    }

    private static FluidStack heaterDisplay(Fluid fluid) {
        ReactorFuel fuel = ReactorFuel.forOutput(fluid);
        if (fuel == null) return new FluidStack(fluid.builtInRegistryHolder(), DISPLAY);
        return CNFluids.spentOrMolten(fluid, fuel.baseComposition(), DISPLAY);
    }
}
