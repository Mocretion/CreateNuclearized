package com.mocretion.createnuclearized.content.boiler;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

public record GasHazards(List<GasEffect> effects, Optional<Damage> damage, float ignite, float potionEffects) {

    private static final double INSTANT_STRENGTH = 0.5;

    public static final MapCodec<GasHazards> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GasEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(GasHazards::effects),
            Damage.CODEC.optionalFieldOf("damage").forGetter(GasHazards::damage),
            Codec.floatRange(0, 60).optionalFieldOf("ignite", 0f).forGetter(GasHazards::ignite),
            Codec.floatRange(0, 10).optionalFieldOf("potion_effects", 0f).forGetter(GasHazards::potionEffects)
    ).apply(i, GasHazards::new));

    public boolean isEmpty() {
        return effects.isEmpty() && damage.isEmpty() && ignite <= 0 && potionEffects <= 0;
    }

    public void apply(LivingEntity entity, FluidStack input) {
        for (GasEffect effect : effects)
            entity.addEffect(effect.instance());
        if (potionEffects > 0)
            applyPotion(entity, input.get(DataComponents.POTION_CONTENTS));
        damage.ifPresent(d -> entity.hurt(new DamageSource(d.type()), d.amount()));
        if (ignite > 0)
            entity.igniteForSeconds(ignite);
    }

    private void applyPotion(LivingEntity entity, PotionContents potion) {
        if (potion == null) return;
        for (MobEffectInstance effect : potion.getAllEffects()) {
            MobEffect mob = effect.getEffect().value();
            if (mob.isInstantenous())
                mob.applyInstantenousEffect(null, null, entity, effect.getAmplifier(), INSTANT_STRENGTH);
            else
                entity.addEffect(new MobEffectInstance(effect.getEffect(), scaledDuration(effect), effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
    }

    private int scaledDuration(MobEffectInstance effect) {
        return effect.mapDuration(duration -> Math.max(1, Math.round(duration * potionEffects)));
    }

    public record Damage(Holder<DamageType> type, float amount) {

        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(i -> i.group(
                DamageType.CODEC.fieldOf("type").forGetter(Damage::type),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("amount").forGetter(Damage::amount)
        ).apply(i, Damage::new));
    }
}
