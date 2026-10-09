package com.mocretion.createnuclearized.content.radiation;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class CNDamageTypes {

    public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "radiation"));

    private CNDamageTypes() {}

    public static DamageSource radiation(Level level) {
        Holder<DamageType> holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(RADIATION);
        return new DamageSource(holder);
    }
}
