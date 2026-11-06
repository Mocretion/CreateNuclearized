package com.mocretion.createnuclearized.content.radiation;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.potion.BlueGlowEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CreateNuclearized.MODID);

    public static final Holder<MobEffect> RADIATION_SICKNESS =
            EFFECTS.register("radiation_sickness", RadiationSicknessEffect::new);

    public static final Holder<MobEffect> BLUE_GLOW =
            EFFECTS.register("blue_glow", BlueGlowEffect::new);

    private CNEffects() {}
}
