package com.mocretion.createnuclearized.content.fluid;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public class SpentSaltBucketWrapper extends FluidBucketWrapper {

    public SpentSaltBucketWrapper(ItemStack container) {
        super(container);
    }

    @Override
    public FluidStack getFluid() {
        FluidStack fluid = super.getFluid();
        if (!fluid.isEmpty()) {
            SaltStats stats = getContainer().get(CNDataComponents.SALT_STATS.get());
            if (stats != null)
                fluid.set(CNDataComponents.SALT_STATS.get(), stats);
        }
        return fluid;
    }
}
