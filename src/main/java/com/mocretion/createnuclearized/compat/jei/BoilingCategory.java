package com.mocretion.createnuclearized.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.content.boiler.BoilingRecipe;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.registry.CNBlocks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class BoilingCategory extends CNRecipeCategory<BoilingRecipe> {

    private static final int HEATER_DISPLAY = 100;
    private static final int BASIN_Y = 31;
    private static final int HEATER_Y = 55;
    private static final float INFO_SCALE = 0.7f;
    private static final int HAZARD_Y = 31;

    private final AnimatedEvaporationBasin basin = new AnimatedEvaporationBasin();
    private final AnimatedIndustrialHeater heater = new AnimatedIndustrialHeater();

    public BoilingCategory(Info<BoilingRecipe> info, IGuiHelper guiHelper) {
        super(info, guiHelper);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BoilingRecipe recipe, IFocusGroup focuses) {
        fluidSlot(builder, RecipeIngredientRole.INPUT, 26, 51, recipe.input(), recipe.maxRate());
        recipe.gas().ifPresent(gas ->
                fluidSlot(builder, RecipeIngredientRole.OUTPUT, 142, 20, new FluidStack(gas.builtInRegistryHolder(), recipe.maxRate())));

        List<FluidStack> heaters = heaterExamples(recipe);
        if (!heaters.isEmpty()) {
            IRecipeSlotBuilder heaterSlot = fluidSlot(builder, RecipeIngredientRole.CATALYST, 26, 75, heaters);
            saltTooltip(heaterSlot);
        }

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 160, 93)
                .addItemStack(new ItemStack(CNBlocks.INDUSTRIAL_HEATER.get()));
    }

    @Override
    public void draw(BoilingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (recipe.gas().isPresent())
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 1);

        int sceneX = getBackground().getWidth() / 2 + 3;

        AllGuiTextures.JEI_LIGHT.render(graphics, sceneX - 10, 88);

        heater.draw(graphics, sceneX, HEATER_Y);
        basin.draw(graphics, sceneX, BASIN_Y);
        risingParticles(graphics, recipe.particles(), slots, sceneX + 14, 22, 52);

        infoBorder(graphics, 0, 0);
        line(graphics, Component.translatable("createnuclearized.jei.rate", recipe.maxRate()), 6, 7, WHITE, INFO_SCALE);
        line(graphics, Component.translatable("createnuclearized.jei.boil_cost", formatFloat(recipe.heatPerMb())), 6, 16, WHITE, INFO_SCALE);
        drawHazards(graphics, recipe.hazards(), HAZARD_X, HAZARD_Y);
    }

    @Override
    protected List<Component> getTooltipStrings(BoilingRecipe recipe, IRecipeSlotsView view, double mouseX, double mouseY) {
        return hazardTooltip(recipe.hazards(), HAZARD_X, HAZARD_Y, mouseX, mouseY);
    }

    private static List<FluidStack> heaterExamples(BoilingRecipe recipe) {
        List<FluidStack> out = new ArrayList<>();
        if (recipe.heater().isEmpty()) return out;
        for (FluidStack stack : recipe.heater().get().getStacks())
            if (stack.getFluid().defaultFluidState().isSource())
                out.add(heaterDisplay(stack.getFluid()));
        return out;
    }

    private static FluidStack heaterDisplay(Fluid fluid) {
        ReactorFuel fuel = ReactorFuel.forOutput(fluid);
        if (fuel == null) return new FluidStack(fluid.builtInRegistryHolder(), HEATER_DISPLAY);
        return CNFluids.spentOrMolten(fluid, fuel.baseComposition(), HEATER_DISPLAY);
    }
}
