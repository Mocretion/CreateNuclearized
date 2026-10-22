package com.mocretion.createnuclearized.content.boiler;

import java.util.Optional;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
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

public record HeatGenerationRecipe(FluidIngredient input, Optional<Integer> heat, Either<HeaterResult, Fluid> output)
        implements Recipe<FluidRecipeInput> {

    private static final Codec<Unit> HEAT_MARKER = Codec.STRING.comapFlatMap(
            s -> "heat".equals(s) ? DataResult.success(Unit.INSTANCE) : DataResult.error(() -> "Expected an integer or \"heat\""),
            u -> "heat");

    private static final Codec<Optional<Integer>> HEAT_CODEC = Codec.either(ExtraCodecs.POSITIVE_INT, HEAT_MARKER)
            .xmap(either -> either.map(Optional::of, unit -> Optional.empty()),
                    heat -> heat.<Either<Integer, Unit>>map(Either::left).orElseGet(() -> Either.right(Unit.INSTANCE)));

    public static final MapCodec<HeatGenerationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(HeatGenerationRecipe::input),
            HEAT_CODEC.fieldOf("heat").forGetter(HeatGenerationRecipe::heat),
            Codec.either(HeaterResult.CODEC, BuiltInRegistries.FLUID.byNameCodec())
                    .optionalFieldOf("output", Either.left(HeaterResult.CONSUMED)).forGetter(HeatGenerationRecipe::output)
    ).apply(i, HeatGenerationRecipe::new));

    public static int temperatureOf(FluidStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getFluidType().getTemperature(stack);
    }

    public int heatFor(FluidStack stack) {
        return heat.orElseGet(() -> temperatureOf(stack));
    }

    public boolean usesFluidHeat() {
        return heat.isEmpty();
    }

    public boolean accepts(FluidStack stack) {
        return !stack.isEmpty() && input.test(stack) && heatFor(stack) > 0;
    }

    public FluidStack output(FluidStack heaterStack, int amount) {
        return output.map(mode -> switch (mode) {
            case CONSUMED -> FluidStack.EMPTY;
            case COOLED -> cooled(heaterStack, amount);
        }, fluid -> new FluidStack(fluid.builtInRegistryHolder(), amount));
    }

    private static FluidStack cooled(FluidStack heaterStack, int amount) {
        SaltStats stats = heaterStack.get(CNDataComponents.SALT_STATS.get());
        if (stats == null) return FluidStack.EMPTY;
        return CNFluids.spentOrMolten(heaterStack.getFluid(), stats.withHeat(0), amount);
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
        return CNRecipeTypes.HEAT_GENERATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CNRecipeTypes.HEAT_GENERATION.get();
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
