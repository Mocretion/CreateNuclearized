package com.mocretion.createnuclearized.content.fluid;

import net.minecraft.world.item.BucketItem;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class DepletedSaltBucketItem extends BucketItem {

    // A subclass, not a plain BucketItem, so NeoForge doesnt auto attach its stateless FluidBucketWrapper (which would drop the
    // SaltStats); we register our own component aware wrapper instead
    public DepletedSaltBucketItem(BaseFlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }
}
