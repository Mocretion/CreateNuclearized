package com.mocretion.createnuclearized.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.content.reactor.SaltStatsTooltip;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@Mixin(BasinBlockEntity.class)
public abstract class BasinGoggleMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"))
    private void createnuclearized$appendSaltStats(List<Component> tooltip, boolean isPlayerSneaking,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!isPlayerSneaking) return;

        BasinBlockEntity self = (BasinBlockEntity) (Object) this;
        appendTank(tooltip, self.getTanks().getFirst());
        appendTank(tooltip, self.getTanks().getSecond());
    }

    private static void appendTank(List<Component> tooltip, SmartFluidTankBehaviour behavior) {
        if (behavior == null) return;
        IFluidHandler handler = behavior.getCapability();
        if (handler == null) return;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            SaltStats stats = fluid.get(CNDataComponents.SALT_STATS.get());
            if (stats != null)
                SaltStatsTooltip.append(tooltip, stats, ReactorFuel.forOutput(fluid.getFluid()));
        }
    }
}
