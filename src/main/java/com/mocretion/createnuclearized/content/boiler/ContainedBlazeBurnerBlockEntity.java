package com.mocretion.createnuclearized.content.boiler;

import java.util.List;

import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class ContainedBlazeBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 1000;
    private static final int WORK_LINGER = 5;

    private FluidTank inputTank;
    private FluidTank outputTank;
    private IFluidHandler fluidHandler;

    private double heaterDebt;
    private int workTimer;

    private boolean active;

    private final LerpedFloat headAnimation = LerpedFloat.linear();
    private final LerpedFloat headAngle = LerpedFloat.angular();

    public ContainedBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        headAngle.startWithValue(0);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        SmartFluidTankBehaviour input = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, CAPACITY, false)
                .forbidExtraction();
        SmartFluidTankBehaviour output = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, CAPACITY, false)
                .forbidInsertion();
        inputTank = input.getPrimaryHandler().setValidator(this::isHeater);
        outputTank = output.getPrimaryHandler();
        fluidHandler = new CombinedTankWrapper(input.getCapability(), output.getCapability());
        behaviours.add(input);
        behaviours.add(output);
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public boolean isActive() {
        return active;
    }

    public float getHeadAnimation(float partialTicks) {
        return headAnimation.getValue(partialTicks);
    }

    public float getHeadAngle(float partialTicks) {
        return headAngle.getValue(partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            animateHead();
            spawnParticles();
        } else {
            tickWork();
        }
    }

    private void spawnParticles() {
        if (level == null) return;
        RandomSource r = level.getRandom();
        if (r.nextInt(4) != 0) return;

        HeatLevel heat = active ? HeatLevel.KINDLED : HeatLevel.SMOULDERING;
        Vec3 c = VecHelper.getCenterOf(worldPosition);
        Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f).multiply(1, 0, 1));
        boolean empty = level.getBlockState(worldPosition.above())
                .getCollisionShape(level, worldPosition.above()).isEmpty();

        if (empty || r.nextInt(8) == 0)
            level.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0, 0, 0);

        if (!heat.isAtLeast(HeatLevel.FADING)) return;

        double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f).multiply(1, .25f, 1).normalize()
                .scale((empty ? .25f : .5) + r.nextDouble() * .125f)).add(0, .5, 0);
        level.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
    }

    private void animateHead() {
        float look = ContainedBlazeBurnerClient.lookAngle(worldPosition);
        float target = Float.isNaN(look) ? 0 : look;
        target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
        headAngle.chase(target, 0.25f, Chaser.exp(5));
        headAngle.tickChaser();

        headAnimation.chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        headAnimation.tickChaser();
    }

    private void tickWork() {
        if (workTimer > 0)
            workTimer--;
        setActive(workTimer > 0);
    }

    public FluidStack getHeater() {
        return inputTank.getFluid();
    }

    public FluidStack getSpent() {
        return outputTank.getFluid();
    }

    public double availableHeater() {
        return inputTank.getFluidAmount() - heaterDebt;
    }

    public boolean spendHeater(double mb, BoilingRecipe recipe) {
        FluidStack heater = inputTank.getFluid();
        if (heater.isEmpty()) return false;
        int whole = (int) (heaterDebt + mb);
        FluidStack spent = whole > 0 ? recipe.spentHeater(heater, whole) : FluidStack.EMPTY;
        if (!spent.isEmpty() && outputTank.fill(spent, FluidAction.SIMULATE) < spent.getAmount()) return false;

        heaterDebt += mb - whole;
        if (whole > 0) {
            inputTank.drain(whole, FluidAction.EXECUTE);
            if (!spent.isEmpty())
                outputTank.fill(spent, FluidAction.EXECUTE);
        }
        workTimer = WORK_LINGER;
        setActive(true);
        return true;
    }

    private void setActive(boolean value) {
        if (active == value) return;
        active = value;
        notifyUpdate();
    }

    private boolean isHeater(FluidStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<BoilingRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.BOILING.get()))
            if (holder.value().acceptsHeater(stack)) return true;
        return false;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidHandler);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Active", active);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        active = tag.getBoolean("Active");
    }
}
