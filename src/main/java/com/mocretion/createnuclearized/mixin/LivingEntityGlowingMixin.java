package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.radiation.CNEffects;

import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public class LivingEntityGlowingMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void createnuclearized$bluebirdGlow(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ((LivingEntity) (Object) this).hasEffect(CNEffects.BLUE_GLOW))
            cir.setReturnValue(true);
    }
}
