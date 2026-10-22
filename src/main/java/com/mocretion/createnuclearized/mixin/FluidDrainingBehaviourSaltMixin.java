package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.fluid.SpentSaltLiquidBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour.BlockPosEntry;

import it.unimi.dsi.fastutil.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(FluidDrainingBehaviour.class)
public abstract class FluidDrainingBehaviourSaltMixin {

    @Shadow
    PriorityQueue<BlockPosEntry> queue;

    @Inject(method = "getDrainableFluid", at = @At("RETURN"))
    private void createnuclearized$stampDrainableSalt(BlockPos rootPos, CallbackInfoReturnable<FluidStack> cir) {
        FluidStack drainable = cir.getReturnValue();
        if (!ReactorFuel.isOutput(drainable.getFluid())) return;
        BlockPos next = queue.isEmpty() ? rootPos : queue.first().pos();
        SaltStats stats = SpentSaltLiquidBlock.statsAt(((FluidDrainingBehaviour) (Object) this).getWorld(), next);
        if (!stats.isEmpty())
            drainable.set(CNDataComponents.SALT_STATS.get(), stats);
    }
}
