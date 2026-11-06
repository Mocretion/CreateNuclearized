package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.radiation.CNEffects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@Mixin(Entity.class)
public class EntityTeamColorClientMixin {

    private static final int BLUEBIRD_GLOW = 0x3C8DFF;

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void createnuclearized$bluebirdGlow(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof LivingEntity living && living.hasEffect(CNEffects.BLUE_GLOW))
            cir.setReturnValue(BLUEBIRD_GLOW);
    }
}
