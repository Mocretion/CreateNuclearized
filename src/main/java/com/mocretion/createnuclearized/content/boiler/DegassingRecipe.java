package com.mocretion.createnuclearized.content.boiler;

import java.util.Optional;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public record DegassingRecipe(FluidIngredient input, Fluid gas, Optional<GasSource> source, float rate, float yield,
                              GasHazards hazards, GasParticles particles) implements Recipe<FluidRecipeInput> {

    public static final MapCodec<DegassingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(DegassingRecipe::input),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("gas").forGetter(DegassingRecipe::gas),
            GasSource.CODEC.optionalFieldOf("source").forGetter(DegassingRecipe::source),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("rate").forGetter(DegassingRecipe::rate),
            Codec.floatRange(0, 1).optionalFieldOf("yield", 1f).forGetter(DegassingRecipe::yield),
            GasHazards.MAP_CODEC.forGetter(DegassingRecipe::hazards),
            GasParticles.CODEC.optionalFieldOf("particles", GasParticles.of(ParticleTypes.SMOKE)).forGetter(DegassingRecipe::particles)
    ).apply(i, DegassingRecipe::new));

    public boolean accepts(FluidStack stack) {
        return input.test(stack) && source.map(s -> s.share(stack) > 0).orElse(true);
    }

    public float gasShare(FluidStack stack) {
        return source.map(s -> s.share(stack) * yield).orElse(yield);
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
        return CNRecipeTypes.DEGASSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CNRecipeTypes.DEGASSING.get();
    }

    public enum GasSource implements StringRepresentable {
        OFF_GAS("off_gas");

        public static final Codec<GasSource> CODEC = StringRepresentable.fromEnum(GasSource::values);

        private final String name;

        GasSource(String name) {
            this.name = name;
        }

        public float share(FluidStack stack) {
            SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
            if (stats == null) return 0;
            return stats.offGas() / 100f;
        }

        public FluidStack strip(FluidStack stack, int amount) {
            SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
            if (stats == null) return stack.copyWithAmount(amount);
            SaltStats stripped = new SaltStats(stats.heat(), stats.uranium(), stats.plutonium(), stats.thorium(), stats.americium(), 0);
            return CNFluids.spentOrMolten(stack.getFluid(), stripped, amount);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
