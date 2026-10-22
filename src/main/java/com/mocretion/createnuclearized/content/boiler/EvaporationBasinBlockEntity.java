package com.mocretion.createnuclearized.content.boiler;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.content.reactor.SaltStatsTooltip;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class EvaporationBasinBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 100;
    private static final int MAX_HOOD_GAP = 8;
    private static final int EFFECT_INTERVAL = 10;

    private FluidTank inputTank;
    private FluidTank outputTank;
    private IFluidHandler fluidHandler;
    private FilteringBehaviour filtering;

    private boolean blocked;

    private double degasProgress;
    private double gasCarry;
    private int effectCooldown;

    public EvaporationBasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        SmartFluidTankBehaviour input = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, CAPACITY, false)
                .forbidExtraction();
        SmartFluidTankBehaviour output = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, CAPACITY, false)
                .forbidInsertion();
        inputTank = input.getPrimaryHandler().setValidator(this::accepts);
        outputTank = output.getPrimaryHandler();
        fluidHandler = new CombinedTankWrapper(input.getCapability(), output.getCapability());
        filtering = new FilteringBehaviour(this, new BasinValueBox()).forRecipes();
        behaviours.add(filtering);
        behaviours.add(input);
        behaviours.add(output);
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;
        if (effectCooldown > 0)
            effectCooldown--;
        FluidStack in = inputTank.getFluid();
        DegassingRecipe degassing = in.isEmpty() || tryBoil(in) ? null : findDegassing(in);
        if (degassing == null)
            setBlocked(false);
        else
            degas(degassing, in);
    }

    private boolean tryBoil(FluidStack in) {
        if (!(level.getBlockEntity(worldPosition.below()) instanceof IndustrialHeaterBlockEntity burner)) return false;
        BoilingRecipe boiling = findBoiling(in, burner);
        return boiling != null && boil(boiling, burner);
    }

    private boolean boil(BoilingRecipe recipe, IndustrialHeaterBlockEntity burner) {
        int maxWater = Math.min(recipe.maxRate(), inputTank.getFluidAmount());
        if (maxWater <= 0) return false;
        int amount = burner.boil(maxWater, recipe);
        if (amount <= 0) return false;

        inputTank.drain(amount, FluidAction.EXECUTE);
        releaseGas(new FluidStack(recipe.gas().builtInRegistryHolder(), amount), recipe.particles(), recipe.hazards());
        return true;
    }

    private void degas(DegassingRecipe recipe, FluidStack in) {
        degasProgress = Math.min(degasProgress + recipe.rate(), recipe.rate() + 1);
        int amount = Math.min((int) degasProgress, in.getAmount());
        if (amount <= 0) return;

        double gasExact = amount * recipe.gasShare(in) + gasCarry;
        int gas = (int) gasExact;
        FluidStack rest = recipe.outputLiquid(in, amount, gas);
        if (!rest.isEmpty() && outputTank.fill(rest, FluidAction.SIMULATE) < rest.getAmount()) {
            setBlocked(true);
            return;
        }
        setBlocked(false);

        degasProgress -= amount;
        gasCarry = gasExact - gas;
        inputTank.drain(amount, FluidAction.EXECUTE);
        if (!rest.isEmpty())
            outputTank.fill(rest, FluidAction.EXECUTE);
        FluidStack released = gas > 0 ? new FluidStack(recipe.gas().builtInRegistryHolder(), gas) : FluidStack.EMPTY;
        releaseGas(released, recipe.particles(), recipe.hazards());
    }

    @Nullable
    private BoilingRecipe findBoiling(FluidStack in, IndustrialHeaterBlockEntity burner) {
        for (RecipeHolder<BoilingRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.BOILING.get())) {
            BoilingRecipe recipe = holder.value();
            if (recipe.input().test(in) && filterAccepts(recipe.gas()) && burner.hasHeatFor(recipe)) return recipe;
        }
        return null;
    }

    @Nullable
    private DegassingRecipe findDegassing(FluidStack in) {
        for (RecipeHolder<DegassingRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.DEGASSING.get()))
            if (holder.value().accepts(in) && filterAccepts(holder.value().gas())) return holder.value();
        return null;
    }

    private boolean filterAccepts(Fluid gas) {
        return filtering == null || filtering.test(new FluidStack(gas.builtInRegistryHolder(), 1));
    }

    private boolean accepts(FluidStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<BoilingRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.BOILING.get()))
            if (holder.value().input().test(stack)) return true;
        return findDegassing(stack) != null;
    }

    private void releaseGas(FluidStack gas, GasParticles particles, GasHazards hazards) {
        int ceiling = ceilingGap();
        if (!gas.isEmpty() && level.getBlockEntity(worldPosition.above(ceiling)) instanceof ExhaustHoodBlockEntity hood)
            hood.addGas(gas);
        double topY = worldPosition.getY() + ceiling;
        if (ceiling > 1)
            emitParticles(topY, particles);
        applyHazards(topY, hazards);
    }

    private int ceilingGap() {
        for (int gap = 1; gap < MAX_HOOD_GAP; gap++)
            if (!level.getBlockState(worldPosition.above(gap)).isAir()) return gap;
        return MAX_HOOD_GAP;
    }

    private void emitParticles(double topY, GasParticles particles) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        RandomSource rand = serverLevel.random;
        double baseY = worldPosition.getY() + 0.9;
        double height = Math.max(0, topY - baseY - 0.75);
        double cx = worldPosition.getX() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        for (int n = 0; n < particles.count(); n++) {
            double y = baseY + rand.nextDouble() * height;
            double x = cx + (rand.nextDouble() - 0.5) * particles.spread();
            double z = cz + (rand.nextDouble() - 0.5) * particles.spread();
            serverLevel.sendParticles(particles.particle(), x, y, z, 0, 0.0, 1.0, 0.0, particles.speed());
        }
    }

    private void applyHazards(double topY, GasHazards hazards) {
        if (hazards.isEmpty() || effectCooldown > 0) return;
        effectCooldown = EFFECT_INTERVAL;
        AABB column = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, topY, worldPosition.getZ() + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, column))
            hazards.apply(entity);
    }

    private void setBlocked(boolean value) {
        if (blocked == value) return;
        blocked = value;
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidHandler);
        appendStats(tooltip, inputTank.getFluid(), "gui.goggles.basin_input");
        appendStats(tooltip, outputTank.getFluid(), "gui.goggles.basin_output");
        if (blocked)
            new LangBuilder(CreateNuclearized.MODID).translate("gui.goggles.basin_blocked")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        return true;
    }

    private static void appendStats(List<Component> tooltip, FluidStack stack, String headerKey) {
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            SaltStatsTooltip.append(tooltip, stats, ReactorFuel.forOutput(stack.getFluid()), headerKey);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Blocked", blocked);
        if (clientPacket) return;
        tag.putDouble("DegasProgress", degasProgress);
        tag.putDouble("GasCarry", gasCarry);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        blocked = tag.getBoolean("Blocked");
        if (clientPacket) return;
        degasProgress = tag.getDouble("DegasProgress");
        gasCarry = tag.getDouble("GasCarry");
    }

    static class BasinValueBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 12, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }
}
