package com.mocretion.createnuclearized.content.compactingfrommelt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mocretion.createnuclearized.CreateNuclearized;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

// Inject liquid compacting recipe i.e. liquid uranium -> raw uranium block into the recipe manager.
// Used to dynamically adjust how much liquid is needed for the block based on how much liquid is yielded
// when converting raw uranium into liquid.
// Lowkey cringe but i guess.
public final class CompactingFromMeltLoader extends SimpleJsonResourceReloadListener {

    public static final String FOLDER = "compacting_from_melt";
    private static final Gson GSON = new Gson();

    public record Entry(ResourceLocation sourceRecipe, int multiplier, ItemStack result) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("source_recipe").forGetter(Entry::sourceRecipe),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("multiplier", 9).forGetter(Entry::multiplier),
            ItemStack.CODEC.fieldOf("result").forGetter(Entry::result)
        ).apply(i, Entry::new));
    }

    private final RecipeManager recipeManager;

    public CompactingFromMeltLoader(RecipeManager recipeManager) {
        super(GSON, FOLDER);
        this.recipeManager = recipeManager;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager rm, ProfilerFiller profiler) {
        if (json.isEmpty()) return;

        List<RecipeHolder<?>> synthesized = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : json.entrySet()) {
            Entry entry = Entry.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                    .resultOrPartial(err -> CreateNuclearized.LOGGER.error(
                            "Failed to parse compacting_from_melt {}: {}", e.getKey(), err))
                    .orElse(null);
            if (entry == null) continue;

            RecipeHolder<?> holder = synthesize(e.getKey(), entry);
            if (holder != null) synthesized.add(holder);
        }

        if (synthesized.isEmpty()) return;

        List<RecipeHolder<?>> all = new ArrayList<>(recipeManager.getRecipes());
        all.addAll(synthesized);
        recipeManager.replaceRecipes(all);
    }

    private RecipeHolder<CompactingRecipe> synthesize(ResourceLocation fileId, Entry entry) {
        Recipe<?> source = recipeManager.byKey(entry.sourceRecipe())
                .map(RecipeHolder::value)
                .orElse(null);
        if (!(source instanceof ProcessingRecipe<?, ?> p)) {
            CreateNuclearized.LOGGER.warn("compacting_from_melt {}: source recipe {} is missing or not a ProcessingRecipe",
                    fileId, entry.sourceRecipe());
            return null;
        }
        List<FluidStack> results = p.getFluidResults();
        if (results.isEmpty() || results.get(0).isEmpty()) {
            CreateNuclearized.LOGGER.warn("compacting_from_melt {}: source recipe {} has no fluid output",
                    fileId, entry.sourceRecipe());
            return null;
        }

        FluidStack needed = results.get(0);
        int amount = needed.getAmount() * entry.multiplier();
        ResourceLocation newId = fileId.withPath(p2 -> FOLDER + "/" + p2);

        CompactingRecipe recipe = new StandardProcessingRecipe.Builder<CompactingRecipe>(CompactingRecipe::new, newId)
                .withFluidIngredients(SizedFluidIngredient.of(needed.getFluid(), amount))
                .withSingleItemOutput(entry.result().copy())
                .build();
        return new RecipeHolder<>(newId, recipe);
    }
}
