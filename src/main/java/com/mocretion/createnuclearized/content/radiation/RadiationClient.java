package com.mocretion.createnuclearized.content.radiation;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerHeartTypeEvent;

@EventBusSubscriber(modid = CreateNuclearized.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class RadiationClient {

    private static final RandomSource RANDOM = RandomSource.create();
    private static final float NUDGE_PER_LEVEL = 0.9f;
    private static final int BASE_INTERVAL = 140;
    private static final float EASE = 0.12f;
    private static final float TARGET_DECAY = 0.88f;

    private static float yawVelocity;
    private static float pitchVelocity;
    private static float yawTarget;
    private static float pitchTarget;
    private static int ticksUntilNudge;

    private RadiationClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) return;

        MobEffectInstance effect = player.getEffect(CNEffects.RADIATION_SICKNESS);
        if (effect == null) {
            reset();
            return;
        }

        int radiationLevel = effect.getAmplifier() + 1;
        if (--ticksUntilNudge <= 0) {
            float magnitude = NUDGE_PER_LEVEL * radiationLevel;
            yawTarget = (RANDOM.nextFloat() * 2f - 1f) * magnitude;
            pitchTarget = (RANDOM.nextFloat() * 2f - 1f) * magnitude * 0.4f;
            ticksUntilNudge = Math.max(BASE_INTERVAL / radiationLevel, 15) + RANDOM.nextInt(20);
        }

        yawVelocity += (yawTarget - yawVelocity) * EASE;
        pitchVelocity += (pitchTarget - pitchVelocity) * EASE;
        yawTarget *= TARGET_DECAY;
        pitchTarget *= TARGET_DECAY;

        player.setYRot(player.getYRot() + yawVelocity);
        player.setXRot(Mth.clamp(player.getXRot() + pitchVelocity, -90f, 90f));
    }

    @SubscribeEvent
    static void onHeartType(PlayerHeartTypeEvent event) {
        if (event.getEntity().hasEffect(CNEffects.RADIATION_SICKNESS))
            event.setType(CNHeartTypes.radioactive());
    }

    private static void reset() {
        yawVelocity = 0f;
        pitchVelocity = 0f;
        yawTarget = 0f;
        pitchTarget = 0f;
        ticksUntilNudge = 0;
    }
}
