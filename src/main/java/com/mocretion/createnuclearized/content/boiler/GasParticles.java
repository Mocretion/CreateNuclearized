package com.mocretion.createnuclearized.content.boiler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

public record GasParticles(ParticleOptions particle, int count, float spread, float speed, boolean potionColor) {

    public static final Codec<GasParticles> CODEC = RecordCodecBuilder.create(i -> i.group(
            ParticleTypes.CODEC.fieldOf("particle").forGetter(GasParticles::particle),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("count", 1).forGetter(GasParticles::count),
            Codec.floatRange(0, 1).optionalFieldOf("spread", 0.4f).forGetter(GasParticles::spread),
            Codec.floatRange(0, 1).optionalFieldOf("speed", 0.03f).forGetter(GasParticles::speed),
            Codec.BOOL.optionalFieldOf("potion_color", false).forGetter(GasParticles::potionColor)
    ).apply(i, GasParticles::new));

    public static GasParticles of(ParticleOptions particle) {
        return new GasParticles(particle, 1, 0.4f, 0.03f, false);
    }

    public ParticleOptions particleFor(FluidStack input) {
        if (!potionColor || !(particle instanceof ColorParticleOption color)) return particle;
        PotionContents potion = input.get(DataComponents.POTION_CONTENTS);
        if (potion == null) return particle;
        int alpha = Math.round(color.getAlpha() * 255);
        return ColorParticleOption.create(color.getType(), (alpha << 24) | (potion.getColor() & 0xFFFFFF));
    }
}
