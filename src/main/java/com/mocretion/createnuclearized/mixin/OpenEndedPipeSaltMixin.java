package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mocretion.createnuclearized.content.fluid.SpentSaltBlockEntity;
import com.mocretion.createnuclearized.content.fluid.SpentSaltLiquidBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.content.fluids.OpenEndedPipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(OpenEndedPipe.class)
public abstract class OpenEndedPipeSaltMixin {

    @Shadow
    private Level world;

    @Shadow
    private BlockPos outputPos;

    @Unique
    private SaltStats createnuclearized$drainedStats = SaltStats.EMPTY;

    @Inject(method = "removeFluidFromSpace", at = @At("HEAD"))
    private void createnuclearized$captureStats(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        createnuclearized$drainedStats = world != null && world.isLoaded(outputPos)
                ? SpentSaltLiquidBlock.statsAt(world, outputPos)
                : SaltStats.EMPTY;
    }

    @Inject(method = "removeFluidFromSpace", at = @At("RETURN"))
    private void createnuclearized$stampStats(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
        FluidStack drained = cir.getReturnValue();
        if (!createnuclearized$drainedStats.isEmpty() && ReactorFuel.isOutput(drained.getFluid()))
            drained.set(CNDataComponents.SALT_STATS.get(), createnuclearized$drainedStats);
    }

    @Inject(method = "provideFluidToSpace", at = @At("RETURN"))
    private void createnuclearized$placeStats(FluidStack fluid, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !cir.getReturnValueZ()) return;
        SaltStats stats = fluid.get(CNDataComponents.SALT_STATS.get());
        if (stats != null && world.getBlockEntity(outputPos) instanceof SpentSaltBlockEntity placed)
            placed.setStats(stats);
    }
}
