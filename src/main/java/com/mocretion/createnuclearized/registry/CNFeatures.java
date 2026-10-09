package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.worldgen.OreClumpFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CreateNuclearized.MODID);

    public static final DeferredHolder<Feature<?>, OreClumpFeature> ORE_CLUMP = FEATURES.register("ore_clump", OreClumpFeature::new);

    private CNFeatures() {}
}
