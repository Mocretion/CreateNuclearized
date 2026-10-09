package com.mocretion.createnuclearized.content.boiler;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public record GasHazards(List<GasEffect> effects, Optional<Damage> damage, float ignite) {

    public static final MapCodec<GasHazards> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GasEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(GasHazards::effects),
            Damage.CODEC.optionalFieldOf("damage").forGetter(GasHazards::damage),
            Codec.floatRange(0, 60).optionalFieldOf("ignite", 0f).forGetter(GasHazards::ignite)
    ).apply(i, GasHazards::new));

    public boolean isEmpty() {
        return effects.isEmpty() && damage.isEmpty() && ignite <= 0;
    }

    public void apply(LivingEntity entity) {
        for (GasEffect effect : effects)
            entity.addEffect(effect.instance());
        damage.ifPresent(d -> entity.hurt(new DamageSource(d.type()), d.amount()));
        if (ignite > 0)
            entity.igniteForSeconds(ignite);
    }

    public record Damage(Holder<DamageType> type, float amount) {

        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(i -> i.group(
                DamageType.CODEC.fieldOf("type").forGetter(Damage::type),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("amount").forGetter(Damage::amount)
        ).apply(i, Damage::new));
    }
}
