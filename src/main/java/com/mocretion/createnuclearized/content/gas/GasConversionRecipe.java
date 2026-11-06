package com.mocretion.createnuclearized.content.gas;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mocretion.createnuclearized.content.recipe.ResultlessRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public record GasConversionRecipe(FluidIngredient gas, Ingredient ingredient, List<ProcessingOutput> results,
                                  boolean copyPotion) implements ResultlessRecipe<SingleRecipeInput> {

    public static final MapCodec<GasConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("gas").forGetter(GasConversionRecipe::gas),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(GasConversionRecipe::ingredient),
            ProcessingOutput.CODEC_NEW.listOf().fieldOf("results").forGetter(GasConversionRecipe::results),
            Codec.BOOL.optionalFieldOf("copy_potion", false).forGetter(GasConversionRecipe::copyPotion)
    ).apply(i, GasConversionRecipe::new));

    public boolean appliesTo(FluidStack carried, ItemStack stack) {
        return gas.test(carried) && ingredient.test(stack);
    }

    public List<ItemStack> roll(RandomSource random, FluidStack carried, int inputCount) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < Math.max(inputCount, 1); i++)
            for (ProcessingOutput output : results) {
                ItemStack rolled = output.rollOutput(random);
                if (rolled.isEmpty()) continue;
                if (copyPotion)
                    stampPotion(rolled, carried);
                merge(out, rolled);
            }
        return out;
    }

    private static void merge(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack existing : stacks) {
            if (stack.isEmpty()) return;
            if (!ItemStack.isSameItemSameComponents(stack, existing)) continue;
            int amount = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
            existing.grow(amount);
            stack.shrink(amount);
        }
        if (!stack.isEmpty())
            stacks.add(stack);
    }

    private static void stampPotion(ItemStack stack, FluidStack carried) {
        PotionContents potion = carried.get(DataComponents.POTION_CONTENTS);
        if (potion != null)
            stack.set(DataComponents.POTION_CONTENTS, potion);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).getStack().copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).getStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CNRecipeTypes.GAS_CONVERSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CNRecipeTypes.GAS_CONVERSION.get();
    }
}
