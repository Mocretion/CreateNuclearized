package com.mocretion.createnuclearized.registry;

import java.util.function.Supplier;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.boiler.BoilingRecipe;
import com.mocretion.createnuclearized.content.boiler.DegassingRecipe;
import com.mocretion.createnuclearized.content.boiler.HeatGenerationRecipe;
import com.mocretion.createnuclearized.content.reconstitution.ReconstitutionRecipe;
import com.mocretion.createnuclearized.content.unmixer.UnmixingRecipe;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CreateNuclearized.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateNuclearized.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BoilingRecipe>> BOILING = type("boiling");
    public static final DeferredHolder<RecipeType<?>, RecipeType<HeatGenerationRecipe>> HEAT_GENERATION = type("heat_generation");
    public static final DeferredHolder<RecipeType<?>, RecipeType<DegassingRecipe>> DEGASSING = type("degassing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<UnmixingRecipe>> UNMIXING = type("unmixing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ReconstitutionRecipe>> RECONSTITUTION = type("reconstitution");

    public static final Supplier<RecipeSerializer<BoilingRecipe>> BOILING_SERIALIZER =
            SERIALIZERS.register("boiling", () -> serializer(BoilingRecipe.CODEC));
    public static final Supplier<RecipeSerializer<HeatGenerationRecipe>> HEAT_GENERATION_SERIALIZER =
            SERIALIZERS.register("heat_generation", () -> serializer(HeatGenerationRecipe.CODEC));
    public static final Supplier<RecipeSerializer<DegassingRecipe>> DEGASSING_SERIALIZER =
            SERIALIZERS.register("degassing", () -> serializer(DegassingRecipe.CODEC));
    public static final Supplier<RecipeSerializer<UnmixingRecipe>> UNMIXING_SERIALIZER =
            SERIALIZERS.register("unmixing", () -> serializer(UnmixingRecipe.CODEC));
    public static final Supplier<RecipeSerializer<ReconstitutionRecipe>> RECONSTITUTION_SERIALIZER =
            SERIALIZERS.register("reconstitution", () -> serializer(ReconstitutionRecipe.CODEC));

    private CNRecipeTypes() {}

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> type(String name) {
        return TYPES.register(name, () -> RecipeType.simple(
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, name)));
    }

    private static <T extends Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec) {
        StreamCodec<RegistryFriendlyByteBuf, T> stream = ByteBufCodecs.fromCodecWithRegistries(codec.codec());
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return stream;
            }
        };
    }
}
