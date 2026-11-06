package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mocretion.createnuclearized.content.gas.GasAirCurrent;
import com.mocretion.createnuclearized.content.gas.GasFanProcessingType;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.AirFlowParticleAccess;

import net.minecraft.util.RandomSource;

@Mixin(AirFlowParticle.class)
public abstract class AirFlowParticleColorMixin {

    @Shadow @Final private IAirCurrentSource source;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/"
            + "processing/FanProcessingType;morphAirFlow(Lcom/simibubi/create/content/kinetics/fan/processing/"
            + "FanProcessingType$AirFlowParticleAccess;Lnet/minecraft/util/RandomSource;)V"))
    private void createnuclearized$gasColor(FanProcessingType type, AirFlowParticleAccess access, RandomSource random,
            Operation<Void> original) {
        original.call(type, access, random);
        if (!(type instanceof GasFanProcessingType)) return;
        AirCurrent current = source.getAirCurrent();
        if (current instanceof GasAirCurrent gas && gas.createnuclearized$gasColor() >= 0)
            access.setColor(gas.createnuclearized$gasColor());
    }
}
