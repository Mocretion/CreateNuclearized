package com.mocretion.createnuclearized.compat.jei;

import java.util.List;

import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.unmixer.UnmixingRecipe;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import javax.annotation.Nullable;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public class UnmixingCategory extends CNRecipeCategory<UnmixingRecipe> {

    private final AnimatedUnmixer mixer = new AnimatedUnmixer();

    public UnmixingCategory(Info<UnmixingRecipe> info, IGuiHelper guiHelper) {
        super(info, guiHelper);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, UnmixingRecipe recipe, IFocusGroup focuses) {
        FluidStack input = inputExample(recipe);
        ReactorFuel fuel = fuelOf(recipe);
        if (input.isEmpty()) {
            fluidSlot(builder, RecipeIngredientRole.INPUT, 18, 51, recipe.input(), recipe.amount());
        } else {
            IRecipeSlotBuilder in = fluidSlot(builder, RecipeIngredientRole.INPUT, 18, 51, input);
            saltTooltip(in, input, fuel);
        }

        List<FluidStack> fluidOuts = recipe.isDynamic() ? recipe.fluidOutputsFor(input) : recipe.fluidResults();
        int i = 0;

        for (int n = 0; n < recipe.itemResults().size(); n++, i++)
            itemSlot(builder, RecipeIngredientRole.OUTPUT, outputX(i), outputY(i), recipe.itemResults().get(n));

        for (int n = 0; n < fluidOuts.size(); n++, i++) {
            IRecipeSlotBuilder out = fluidSlot(builder, RecipeIngredientRole.OUTPUT, outputX(i), outputY(i), fluidOuts.get(n));
            saltTooltip(out, fluidOuts.get(n), fuel);
        }
    }

    @Override
    public void draw(UnmixingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 92, 22);
        AllGuiTextures.JEI_SHADOW.render(graphics, 44, 68);

        if (recipe.isDynamic()) {
            renderMirroredX(graphics, AllGuiTextures.JEI_NO_HEAT_BAR, 4, 80);
            line(graphics, Component.translatable("createnuclearized.jei.unmixing_pass"), 9, 86, 0xffffff, 0.9f);
        }

        mixer.draw(graphics, 54, 34);
    }

    private static int outputX(int i) {
        return 98 + (i % 4) * 19;
    }

    private static int outputY(int i) {
        return 40 + (i / 4) * 19;
    }

    @Nullable
    private static ReactorFuel fuelOf(UnmixingRecipe recipe) {
        FluidStack[] stacks = recipe.input().getStacks();
        return stacks.length == 0 ? null : ReactorFuel.forOutput(stacks[0].getFluid());
    }

    private static FluidStack inputExample(UnmixingRecipe recipe) {
        if (!recipe.isDynamic()) return FluidStack.EMPTY;
        FluidStack[] stacks = recipe.input().getStacks();
        if (stacks.length == 0) return FluidStack.EMPTY;
        ReactorFuel fuel = ReactorFuel.forOutput(stacks[0].getFluid());
        if (fuel == null) return FluidStack.EMPTY;
        return CNFluids.spentOrMolten(stacks[0].getFluid(), fuel.baseComposition(), recipe.amount());
    }
}
