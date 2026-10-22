package com.mocretion.createnuclearized.content.radiation;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class RadiationSicknessEffect extends MobEffect {

    private static final int COLOR = 0x3BE05A;
    private static final int DAMAGE_INTERVAL = 40;
    private static final float DAMAGE = 1.0f;

    public RadiationSicknessEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(CNDamageTypes.radiation(entity.level()), DAMAGE);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = amplifier >= Integer.SIZE ? 1 : Math.max(DAMAGE_INTERVAL >> amplifier, 1);
        return duration % interval == 0;
    }
}
