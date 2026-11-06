package com.mocretion.createnuclearized.content.gas;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.boiler.EvaporationBasinBlockEntity;
import com.mocretion.createnuclearized.content.recipe.CachedRecipes;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class GasFanProcessingType implements FanProcessingType {

    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        return EvaporationBasinBlockEntity.emittingBasinBelow(level, pos) != null;
    }

    @Override
    public int getPriority() {
        return 350;
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return findRecipe(stack, level) != null;
    }

    @Override
    @Nullable
    public List<ItemStack> process(ItemStack stack, Level level) {
        FluidStack carried = carriedGas(level);
        GasConversionRecipe recipe = findRecipe(carried, stack, level);
        if (recipe == null) return null;
        return recipe.roll(level.random, carried, stack.getCount());
    }

    @Nullable
    private static GasConversionRecipe findRecipe(ItemStack stack, Level level) {
        return findRecipe(carriedGas(level), stack, level);
    }

    @Nullable
    private static GasConversionRecipe findRecipe(FluidStack carried, ItemStack stack, Level level) {
        if (carried.isEmpty()) return null;
        for (RecipeHolder<GasConversionRecipe> holder : CachedRecipes.of(level, CNRecipeTypes.GAS_CONVERSION.get()))
            if (holder.value().appliesTo(carried, stack)) return holder.value();
        return null;
    }

    private static FluidStack carriedGas(Level level) {
        GasAirCurrent current = GasAirCurrent.TICKING.get();
        if (current == null) return FluidStack.EMPTY;
        BlockPos basinPos = current.createnuclearized$gasBasin();
        if (basinPos == null || !(level.getBlockEntity(basinPos) instanceof EvaporationBasinBlockEntity basin) || !basin.isEmittingGas())
            return FluidStack.EMPTY;
        return basin.getCarriedGas();
    }

    @Override
    public void spawnProcessingParticles(Level level, Vec3 pos) {
        if (level.random.nextInt(6) != 0) return;
        GasAirCurrent current = GasAirCurrent.TICKING.get();
        if (current == null) return;
        BlockPos basinPos = current.createnuclearized$gasBasin();
        if (basinPos == null || !(level.getBlockEntity(basinPos) instanceof EvaporationBasinBlockEntity basin)) return;
        ParticleOptions particle = basin.getBeamParticle();
        if (particle == null) return;
        level.addParticle(particle, pos.x, pos.y + .25f, pos.z, 0, 1 / 16f, 0);
    }

    @Override
    public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
        particleAccess.setColor(Color.mixColors(0xB8C4BE, 0xE8EFEA, random.nextFloat()));
        particleAccess.setAlpha(.75f);
    }

    @Override
    public void affectEntity(Entity entity, Level level) {
        // Living-entity hazards are applied by the basin along the diverted beam.
    }
}
