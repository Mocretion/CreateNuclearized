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
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankGoggleMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"))
    private void createnuclearized$appendSaltStats(List<Component> tooltip, boolean isPlayerSneaking,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;

        if (!isPlayerSneaking) return;

        FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
        FluidTankBlockEntity controller = self.getControllerBE();
        if (controller == null) return;

        FluidStack fluid = controller.getTankInventory().getFluid();
        SaltStats stats = fluid.get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            SaltStatsTooltip.append(tooltip, stats, ReactorFuel.forOutput(fluid.getFluid()));
    }
}
