package com.mocretion.createnuclearized.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mocretion.createnuclearized.content.fluid.PendingSaltStats;
import com.mocretion.createnuclearized.content.fluid.SpentSaltBlockEntity;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(FluidFillingBehaviour.class)
public abstract class FluidFillingBehaviourSaltMixin implements PendingSaltStats {

    @Unique
    private SaltStats createnuclearized$pendingStats = SaltStats.EMPTY;

    @Override
    public void createnuclearized$setPendingStats(SaltStats stats) {
        createnuclearized$pendingStats = stats;
    }

    @WrapOperation(method = "tryDeposit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock("
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean createnuclearized$stampPlacedSalt(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        boolean placed = original.call(level, pos, state, flags);
        if (placed && !createnuclearized$pendingStats.isEmpty() && level.getBlockEntity(pos) instanceof SpentSaltBlockEntity salt)
            salt.setStats(createnuclearized$pendingStats);
        return placed;
    }
}
