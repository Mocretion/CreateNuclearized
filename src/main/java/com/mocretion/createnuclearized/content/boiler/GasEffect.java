package com.mocretion.createnuclearized.content.boiler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public record GasEffect(Holder<MobEffect> effect, int amplifier, int duration) {

    public static final Codec<GasEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(GasEffect::effect),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amplifier", 0).forGetter(GasEffect::amplifier),
            ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(GasEffect::duration)
    ).apply(i, GasEffect::new));

    public MobEffectInstance instance() {
        return new MobEffectInstance(effect, duration, amplifier);
    }
}
