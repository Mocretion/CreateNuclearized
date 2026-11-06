package com.mocretion.createnuclearized.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.content.gas.GasConversionRecipe;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

public class GasConversionCategory extends CNRecipeCategory<GasConversionRecipe> {

    private static final int DISPLAY_BATCH = 1000;

    public GasConversionCategory(Info<GasConversionRecipe> info, IGuiHelper guiHelper) {
        super(info, guiHelper);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GasConversionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 18)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(recipe.ingredient());

        if (recipe.copyPotion() && gasIsPotion(recipe)) {
            List<PotionContents> potions = potionContents();
            List<FluidStack> gasStacks = new ArrayList<>();
            for (PotionContents potion : potions)
                gasStacks.add(PotionFluid.of(DISPLAY_BATCH, potion, BottleType.REGULAR));
            hideFluidAmount(fluidSlot(builder, RecipeIngredientRole.INPUT, 27, 42, gasStacks));

            for (int n = 0; n < recipe.results().size(); n++) {
                ProcessingOutput output = recipe.results().get(n);
                List<ItemStack> outs = new ArrayList<>();
                for (PotionContents potion : potions) {
                    ItemStack stack = output.getStack().copy();
                    stack.set(DataComponents.POTION_CONTENTS, potion);
                    outs.add(stack);
                }
                builder.addSlot(RecipeIngredientRole.OUTPUT, outputX(n), outputY(n))
                        .setBackground(getRenderedSlot(output), -1, -1)
                        .addItemStacks(outs)
                        .addRichTooltipCallback(addStochasticTooltip(output));
            }
            return;
        }

        IRecipeSlotBuilder gas = fluidSlot(builder, RecipeIngredientRole.INPUT, 27, 42, recipe.gas(), 1);
        hideFluidAmount(gas);
        saltTooltip(gas);
        for (int n = 0; n < recipe.results().size(); n++)
            itemSlot(builder, RecipeIngredientRole.OUTPUT, outputX(n), outputY(n), recipe.results().get(n));
    }

    @Override
    public void draw(GasConversionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 58, 22);
        line(graphics, Component.translatable("createnuclearized.jei.gas_conversion_note"), 6, 62, TEXT_COLOR, 0.85f);
        if (recipe.copyPotion())
            line(graphics, Component.translatable("createnuclearized.jei.gas_conversion_potion"), 6, 72, TEXT_COLOR, 0.85f);
    }

    private static int outputX(int i) {
        return 100 + (i % 4) * 19;
    }

    private static int outputY(int i) {
        return 20 + (i / 4) * 19;
    }

    private static boolean gasIsPotion(GasConversionRecipe recipe) {
        for (FluidStack stack : recipe.gas().getStacks())
            if (stack.getFluid() instanceof PotionFluid) return true;
        return false;
    }

    private static List<PotionContents> potionContents() {
        List<PotionContents> out = new ArrayList<>();
        BuiltInRegistries.POTION.holders()
                .filter(potion -> !potion.value().getEffects().isEmpty())
                .forEach(potion -> out.add(new PotionContents(potion)));
        return out;
    }
}
