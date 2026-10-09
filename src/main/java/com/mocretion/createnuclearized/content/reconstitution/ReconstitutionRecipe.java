package com.mocretion.createnuclearized.content.reconstitution;

import java.util.Locale;

import org.apache.commons.lang3.EnumUtils;

import com.mocretion.createnuclearized.content.boiler.FluidRecipeInput;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public record ReconstitutionRecipe(FluidIngredient input, ReactorFuel.Element element, Fluid liquid, Fluid result, int amount)
        implements Recipe<FluidRecipeInput> {

    private static final Codec<ReactorFuel.Element> ELEMENT_CODEC = Codec.stringResolver(
            e -> e.name().toLowerCase(Locale.ROOT),
            s -> EnumUtils.getEnum(ReactorFuel.Element.class, s.toUpperCase(Locale.ROOT)));

    public static final MapCodec<ReconstitutionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(ReconstitutionRecipe::input),
            ELEMENT_CODEC.fieldOf("element").forGetter(ReconstitutionRecipe::element),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("liquid").forGetter(ReconstitutionRecipe::liquid),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("result").forGetter(ReconstitutionRecipe::result),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("amount", 1000).forGetter(ReconstitutionRecipe::amount)
    ).apply(i, ReconstitutionRecipe::new));

    public boolean accepts(FluidStack stack) {
        if (!input.test(stack)) return false;
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats == null || stats.heat() != 0 || stats.offGas() != 0) return false;
        for (ReactorFuel.Element e : ReactorFuel.Element.values()) {
            int amt = stats.amount(e);
            boolean valid = e == element ? amt > 0 && amt < 100 : amt == 0;  // Only accepts if fluid has leftovers
            if (!valid) return false;
        }
        return true;
    }

    public int neededLiquid(SaltStats stats) {
        return (100 - stats.amount(element)) * amount / 100;
    }

    public FluidStack resultStack() {
        return new FluidStack(result.builtInRegistryHolder(), amount);
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
        return CNRecipeTypes.RECONSTITUTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CNRecipeTypes.RECONSTITUTION.get();
    }
}
