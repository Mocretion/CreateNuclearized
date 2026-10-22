package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public record BoilingRecipe(FluidIngredient input, Fluid gas, Optional<FluidIngredient> heater, float heatPerMb, int maxRate,
                            GasHazards hazards, GasParticles particles)
        implements Recipe<FluidRecipeInput> {

    public static final MapCodec<BoilingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(BoilingRecipe::input),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("gas").forGetter(BoilingRecipe::gas),
            FluidIngredient.CODEC_NON_EMPTY.optionalFieldOf("heater").forGetter(BoilingRecipe::heater),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("heat_per_mb").forGetter(BoilingRecipe::heatPerMb),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_rate", 10).forGetter(BoilingRecipe::maxRate),
            GasHazards.MAP_CODEC.forGetter(BoilingRecipe::hazards),
            GasParticles.CODEC.optionalFieldOf("particles", GasParticles.of(ParticleTypes.CLOUD)).forGetter(BoilingRecipe::particles)
    ).apply(i, BoilingRecipe::new));

    @Override
    public boolean matches(FluidRecipeInput in, Level level) {
        return input.test(in.fluid());
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
        return CNRecipeTypes.BOILING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CNRecipeTypes.BOILING.get();
    }
}
