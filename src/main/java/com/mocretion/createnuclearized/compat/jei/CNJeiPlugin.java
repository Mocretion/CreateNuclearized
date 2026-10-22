package com.mocretion.createnuclearized.compat.jei;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.boiler.BoilingRecipe;
import com.mocretion.createnuclearized.content.boiler.DegassingRecipe;
import com.mocretion.createnuclearized.content.boiler.HeatGenerationRecipe;
import com.mocretion.createnuclearized.content.reconstitution.ReconstitutionRecipe;
import com.mocretion.createnuclearized.content.unmixer.UnmixingRecipe;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CNJeiPlugin implements IModPlugin {

    private CreateRecipeCategory<BoilingRecipe> boiling;
    private CreateRecipeCategory<HeatGenerationRecipe> heatGeneration;
    private CreateRecipeCategory<DegassingRecipe> degassing;
    private CreateRecipeCategory<UnmixingRecipe> unmixing;
    private CreateRecipeCategory<ReconstitutionRecipe> reconstitution;

    @Override
    public ResourceLocation getPluginUid() {
        return rl("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        boiling = new CreateRecipeCategory.Builder<BoilingRecipe>(BoilingRecipe.class)
                .addTypedRecipes(() -> CNRecipeTypes.BOILING.get())
                .catalyst(() -> CNBlocks.EVAPORATION_BASIN.get())
                .catalyst(() -> CNBlocks.INDUSTRIAL_HEATER.get())
                .catalyst(() -> CNBlocks.EXHAUST_HOOD.get())
                .itemIcon(CNBlocks.EVAPORATION_BASIN.get())
                .emptyBackground(177, 110)
                .build(rl("boiling"), info -> new BoilingCategory(info, guiHelper));

        heatGeneration = new CreateRecipeCategory.Builder<HeatGenerationRecipe>(HeatGenerationRecipe.class)
                .addTypedRecipes(() -> CNRecipeTypes.HEAT_GENERATION.get())
                .catalyst(() -> CNBlocks.INDUSTRIAL_HEATER.get())
                .itemIcon(CNBlocks.INDUSTRIAL_HEATER.get())
                .emptyBackground(177, 71)
                .build(rl("heat_generation"), info -> new HeatGenerationCategory(info, guiHelper));

        degassing = new CreateRecipeCategory.Builder<DegassingRecipe>(DegassingRecipe.class)
                .addTypedRecipes(() -> CNRecipeTypes.DEGASSING.get())
                .catalyst(() -> CNBlocks.EVAPORATION_BASIN.get())
                .catalyst(() -> CNBlocks.EXHAUST_HOOD.get())
                .itemIcon(CNBlocks.EVAPORATION_BASIN.get())
                .emptyBackground(177, 88)
                .build(rl("degassing"), info -> new DegassingCategory(info, guiHelper));

        unmixing = new CreateRecipeCategory.Builder<UnmixingRecipe>(UnmixingRecipe.class)
                .addTypedRecipes(() -> CNRecipeTypes.UNMIXING.get())
                .catalyst(() -> CNBlocks.PRECISION_UNMIXER.get())
                .itemIcon(CNBlocks.PRECISION_UNMIXER.get())
                .emptyBackground(177, 100)
                .build(rl("unmixing"), info -> new UnmixingCategory(info, guiHelper));

        reconstitution = new CreateRecipeCategory.Builder<ReconstitutionRecipe>(ReconstitutionRecipe.class)
                .addTypedRecipes(() -> CNRecipeTypes.RECONSTITUTION.get())
                .catalyst(() -> AllBlocks.MECHANICAL_MIXER.get())
                .doubleItemIcon(AllBlocks.MECHANICAL_MIXER.get(), CNFluids.uraniumSalt().getBucket())
                .emptyBackground(177, 100)
                .build(rl("reconstitution"), info -> new ReconstitutionCategory(info, guiHelper));

        registration.addRecipeCategories(boiling, heatGeneration, degassing, unmixing, reconstitution);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        boiling.registerRecipes(registration);
        heatGeneration.registerRecipes(registration);
        degassing.registerRecipes(registration);
        unmixing.registerRecipes(registration);
        reconstitution.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        boiling.registerCatalysts(registration);
        heatGeneration.registerCatalysts(registration);
        degassing.registerCatalysts(registration);
        unmixing.registerCatalysts(registration);
        reconstitution.registerCatalysts(registration);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, path);
    }
}
