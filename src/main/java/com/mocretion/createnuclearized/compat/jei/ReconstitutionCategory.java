package com.mocretion.createnuclearized.compat.jei;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.content.reconstitution.ReconstitutionRecipe;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.simibubi.create.compat.jei.category.animations.AnimatedMixer;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public class ReconstitutionCategory extends CNRecipeCategory<ReconstitutionRecipe> {

    private static final int FALLBACK_LEFTOVER = 50;

    private final AnimatedMixer mixer = new AnimatedMixer();

    public ReconstitutionCategory(Info<ReconstitutionRecipe> info, IGuiHelper guiHelper) {
        super(info, guiHelper);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReconstitutionRecipe recipe, IFocusGroup focuses) {
        SaltStats leftover = leftoverStats(recipe);
        FluidStack drained = drainedSalt(recipe, leftover);
        int needed = recipe.neededLiquid(leftover);

        if (drained.isEmpty()) {
            fluidSlot(builder, RecipeIngredientRole.INPUT, 26, 51, recipe.input(), recipe.amount());
        } else {
            IRecipeSlotBuilder saltSlot = fluidSlot(builder, RecipeIngredientRole.INPUT, 26, 51, drained);
            saltTooltip(saltSlot, leftover, fuelOf(recipe));
        }

        fluidSlot(builder, RecipeIngredientRole.INPUT, 45, 51, new FluidStack(recipe.liquid().builtInRegistryHolder(), needed));
        fluidSlot(builder, RecipeIngredientRole.OUTPUT, 142, 51, recipe.resultStack());
    }

    @Override
    public void draw(ReconstitutionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);
        AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);

        AllGuiTextures.JEI_NO_HEAT_BAR.render(graphics, 4, 80);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("createnuclearized.jei.reconstitution_note"), 9,
                86, 0xffffff, false);

        mixer.draw(graphics, getBackground().getWidth() / 2 + 3, 34);
    }

    private static SaltStats leftoverStats(ReconstitutionRecipe recipe) {
        int leftover = FALLBACK_LEFTOVER;
        ReactorFuel fuel = fuelOf(recipe);
        if (fuel != null) leftover = fuel.baseComposition().amount(recipe.element());
        return SaltStats.EMPTY.withElement(recipe.element(), Math.max(leftover, 1));
    }

    private static FluidStack drainedSalt(ReconstitutionRecipe recipe, SaltStats leftover) {
        FluidStack[] stacks = recipe.input().getStacks();
        if (stacks.length == 0) return FluidStack.EMPTY;
        return CNFluids.spentOrMolten(stacks[0].getFluid(), leftover, recipe.amount());
    }

    @Nullable
    private static ReactorFuel fuelOf(ReconstitutionRecipe recipe) {
        FluidStack[] stacks = recipe.input().getStacks();
        return stacks.length == 0 ? null : ReactorFuel.forOutput(stacks[0].getFluid());
    }
}
