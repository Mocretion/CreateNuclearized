package com.mocretion.createnuclearized.content.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BlueGlowEffect extends MobEffect {

    private static final int COLOR = 0x3C8DFF;

    public BlueGlowEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }
}
