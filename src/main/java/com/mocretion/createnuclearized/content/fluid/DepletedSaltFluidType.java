package com.mocretion.createnuclearized.content.fluid;

import java.util.function.Consumer;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class DepletedSaltFluidType extends FluidType {

    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;

    public DepletedSaltFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
    }

    // getFilledBucket routes through here, so this is where a spouted/filled bucket keeps the salts SaltStats
    @Override
    public ItemStack getBucket(FluidStack stack) {
        ItemStack bucket = new ItemStack(stack.getFluid().getBucket());
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            bucket.set(CNDataComponents.SALT_STATS.get(), stats);
        return bucket;
    }

    // Same texture wiring Registrate's default type does
    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }
        });
    }
}
