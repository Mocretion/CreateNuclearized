package com.mocretion.createnuclearized.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.content.boiler.DegassingRecipe;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public class DegassingCategory extends CNRecipeCategory<DegassingRecipe> {

    private static final int DISPLAY_BATCH = 1000;
    private static final int BASIN_Y = 34;
    private static final float INFO_SCALE = 0.7f;
    private static final int HAZARD_Y = 25;

    private final AnimatedEvaporationBasin basin = new AnimatedEvaporationBasin();

    public DegassingCategory(Info<DegassingRecipe> info, IGuiHelper guiHelper) {
        super(info, guiHelper);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DegassingRecipe recipe, IFocusGroup focuses) {
        List<FluidStack> inputs = inputExamples(recipe);
        IRecipeSlotBuilder inputSlot = fluidSlot(builder, RecipeIngredientRole.INPUT, 26, 51, inputs);
        if (recipe.producesOffGas()) saltTooltip(inputSlot);

        fluidSlot(builder, RecipeIngredientRole.OUTPUT, 142, 20, gasExamples(recipe, inputs));

        if (recipe.hasOutputLiquid()) {
            List<FluidStack> stripped = strippedExamples(recipe, inputs);
            if (!stripped.isEmpty()) {
                IRecipeSlotBuilder strippedSlot = fluidSlot(builder, RecipeIngredientRole.OUTPUT, 142, 60, stripped);
                saltTooltip(strippedSlot);
            }
        }
    }

    @Override
    public void draw(DegassingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 1);
        if (recipe.hasOutputLiquid())
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 41);

        int sceneX = getBackground().getWidth() / 2 + 3;

        AllGuiTextures.JEI_SHADOW.render(graphics, sceneX - 10, 68);

        basin.draw(graphics, sceneX, BASIN_Y);
        risingParticles(graphics, recipe.particles(), sceneX + 14, 22, 52);

        infoBorder(graphics, 0, 0);
        line(graphics, Component.translatable("createnuclearized.jei.rate", formatFloat(recipe.rate())), 6, 7, WHITE, INFO_SCALE);
        line(graphics, Component.translatable("createnuclearized.jei.yield", Math.round(recipe.yield() * 100)), 6, 16, WHITE,
                INFO_SCALE);
        drawHazards(graphics, recipe.hazards(), HAZARD_X, HAZARD_Y);
    }

    @Override
    protected List<Component> getTooltipStrings(DegassingRecipe recipe, IRecipeSlotsView view, double mouseX, double mouseY) {
        return hazardTooltip(recipe.hazards(), HAZARD_X, HAZARD_Y, mouseX, mouseY);
    }

    private static List<FluidStack> inputExamples(DegassingRecipe recipe) {
        List<FluidStack> out = new ArrayList<>();
        boolean salt = recipe.producesOffGas();
        for (FluidStack stack : recipe.input().getStacks()) {
            if (!stack.getFluid().defaultFluidState().isSource()) continue;
            ReactorFuel fuel = salt ? ReactorFuel.forOutput(stack.getFluid()) : null;
            if (fuel == null)
                out.add(stack.copyWithAmount(DISPLAY_BATCH));
            else
                out.add(CNFluids.spentOrMolten(stack.getFluid(), fuel.baseComposition(), DISPLAY_BATCH));
        }
        return out;
    }

    private static List<FluidStack> gasExamples(DegassingRecipe recipe, List<FluidStack> inputs) {
        List<FluidStack> out = new ArrayList<>();
        for (FluidStack input : inputs)
            out.add(new FluidStack(recipe.gas().builtInRegistryHolder(), Math.max(displayGas(recipe, input), 1)));
        if (out.isEmpty())
            out.add(new FluidStack(recipe.gas().builtInRegistryHolder(), Math.max(Math.round(DISPLAY_BATCH * recipe.yield()), 1)));
        return out;
    }

    private static List<FluidStack> strippedExamples(DegassingRecipe recipe, List<FluidStack> inputs) {
        List<FluidStack> out = new ArrayList<>();
        for (FluidStack input : inputs) {
            if (input.isEmpty()) continue;
            FluidStack stripped = recipe.outputLiquid(input, DISPLAY_BATCH, displayGas(recipe, input));
            if (!stripped.isEmpty()) out.add(stripped);
        }
        return out;
    }

    private static int displayGas(DegassingRecipe recipe, FluidStack input) {
        return Math.round(DISPLAY_BATCH * recipe.gasShare(input));
    }
}
