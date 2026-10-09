package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mojang.datafixers.util.Either;
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

public record BoilingRecipe(FluidIngredient input, Fluid gas, FluidIngredient heater, int minHeat, float heatPerMb, int maxRate,
                            Either<HeaterResult, Fluid> heaterResult, GasHazards hazards, GasParticles particles)
        implements Recipe<FluidRecipeInput> {

    public static final MapCodec<BoilingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(BoilingRecipe::input),
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("gas").forGetter(BoilingRecipe::gas),
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("heater").forGetter(BoilingRecipe::heater),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_heat", 1).forGetter(BoilingRecipe::minHeat),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("heat_per_mb").forGetter(BoilingRecipe::heatPerMb),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_rate", 10).forGetter(BoilingRecipe::maxRate),
            Codec.either(HeaterResult.CODEC, BuiltInRegistries.FLUID.byNameCodec())
                    .optionalFieldOf("heater_result", Either.left(HeaterResult.COOLED)).forGetter(BoilingRecipe::heaterResult),
            GasHazards.MAP_CODEC.forGetter(BoilingRecipe::hazards),
            GasParticles.CODEC.optionalFieldOf("particles", GasParticles.of(ParticleTypes.CLOUD)).forGetter(BoilingRecipe::particles)
    ).apply(i, BoilingRecipe::new));

    public static int temperatureOf(FluidStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getFluidType().getTemperature(stack);
    }

    public boolean acceptsHeater(FluidStack stack) {
        return heater.test(stack) && temperatureOf(stack) >= minHeat;
    }

    public FluidStack spentHeater(FluidStack heaterStack, int amount) {
        return heaterResult.map(mode -> switch (mode) {
            case CONSUMED -> FluidStack.EMPTY;
            case COOLED -> cooled(heaterStack, amount);
        }, fluid -> new FluidStack(fluid.builtInRegistryHolder(), amount));
    }

    private static FluidStack cooled(FluidStack heaterStack, int amount) {
        SaltStats stats = heaterStack.get(CNDataComponents.SALT_STATS.get());
        if (stats == null) return FluidStack.EMPTY;
        SaltStats cooled = new SaltStats(0, stats.uranium(), stats.plutonium(), stats.thorium(), stats.americium(), stats.offGas());
        return CNFluids.spentOrMolten(heaterStack.getFluid(), cooled, amount);
    }

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

    public enum HeaterResult implements StringRepresentable {
        COOLED("cooled"),
        CONSUMED("consumed");

        public static final Codec<HeaterResult> CODEC = StringRepresentable.fromEnum(HeaterResult::values);

        private final String name;

        HeaterResult(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
