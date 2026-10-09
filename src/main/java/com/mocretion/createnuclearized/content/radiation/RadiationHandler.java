package com.mocretion.createnuclearized.content.radiation;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.fluid.TexturedFluidType;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CreateNuclearized.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class RadiationHandler {

    private static final int APPLY_DURATION = 400;
    private static final int REFRESH_BELOW = 60;
    private static final float BURN_SECONDS = 8.0f;

    private RadiationHandler() {}

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        Level level = living.level();
        if (level.isClientSide) return;
        if (!(living.getMaxHeightFluidType() instanceof TexturedFluidType fluid) || fluid.getRadiationLevel() <= 0) return;

        int amplifier = fluid.getRadiationLevel() - 1;
        MobEffectInstance current = living.getEffect(CNEffects.RADIATION_SICKNESS);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < REFRESH_BELOW) {
            living.addEffect(new MobEffectInstance(CNEffects.RADIATION_SICKNESS, APPLY_DURATION, amplifier, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, APPLY_DURATION, 0, false, false, false));
        }

        if (fluid.burns()) living.igniteForSeconds(BURN_SECONDS);
    }
}
