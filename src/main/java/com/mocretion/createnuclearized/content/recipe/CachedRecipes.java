package com.mocretion.createnuclearized.content.recipe;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class CachedRecipes {

    private static final Map<RecipeType<?>, Entry> CACHE = new ConcurrentHashMap<>();

    private record Entry(RecipeManager manager, List<?> recipes) {}

    private CachedRecipes() {}

    @SuppressWarnings("unchecked")
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> of(Level level, RecipeType<T> type) {
        RecipeManager manager = level.getRecipeManager();
        if (level.isClientSide) return manager.getAllRecipesFor(type);
        Entry entry = CACHE.get(type);
        if (entry == null || entry.manager() != manager) {
            entry = new Entry(manager, List.copyOf(manager.getAllRecipesFor(type)));
            CACHE.put(type, entry);
        }
        return (List<RecipeHolder<T>>) entry.recipes();
    }
}
