package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mocretion.createnuclearized.content.fluid.PendingSaltStats;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(HosePulleyFluidHandler.class)
public abstract class HosePulleyFluidHandlerSaltMixin {

    @Shadow
    private SmartFluidTank internalTank;

    @WrapOperation(method = "fill", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/"
            + "FluidFillingBehaviour;tryDeposit(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean createnuclearized$depositWithStats(FluidFillingBehaviour filler, Fluid fluid, BlockPos root, boolean simulate,
                                                       Operation<Boolean> original, @Local(argsOnly = true) FluidStack resource) {
        PendingSaltStats pending = (PendingSaltStats) filler;
        pending.createnuclearized$setPendingStats(resource.getOrDefault(CNDataComponents.SALT_STATS.get(), SaltStats.EMPTY));
        boolean deposited = original.call(filler, fluid, root, simulate);
        pending.createnuclearized$setPendingStats(SaltStats.EMPTY);
        return deposited;
    }

    @WrapOperation(method = "drainInternal", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean createnuclearized$keepSaltBatchesApart(FluidDrainingBehaviour drainer, BlockPos root, boolean simulate,
                                                           Operation<Boolean> original) {
        FluidStack held = internalTank.getFluid();
        if (ReactorFuel.isOutput(held.getFluid()) && !FluidStack.isSameFluidSameComponents(held, drainer.getDrainableFluid(root)))
            return false;
        return original.call(drainer, root, simulate);
    }
}
