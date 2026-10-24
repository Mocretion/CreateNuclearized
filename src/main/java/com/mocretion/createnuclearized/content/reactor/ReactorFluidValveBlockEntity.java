package com.mocretion.createnuclearized.content.reactor;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.menu.CNIcons;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class ReactorFluidValveBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 2000;
    public static final int OUTPUT_MOLTEN_CAPACITY = 100;
    public static final int OUTPUT_HEATED_CAPACITY = 100;

    private static final double RATE_ACCUM_CAP = 10.0;
    private static final int SYNC_INTERVAL = 10;

    public enum DrainSource implements INamedIconOptions {
        OUTPUT(AllIcons.I_PRIORITY_LOW),
        INPUT(CNIcons.BLOCK_INPUT);

        private final AllIcons icon;

        DrainSource(AllIcons icon) {
            this.icon = icon;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return "createnuclearized.salt_valve.drain_source." + name().toLowerCase(Locale.ROOT);
        }
    }

    private final FluidTank moltenTank;
    @Nullable
    private final FluidTank heatedTank;
    private final IFluidHandler fluidHandler = new ValveFluidHandler(true, true);
    private final IFluidHandler fillOnlyHandler = new ValveFluidHandler(true, false);
    private final IFluidHandler drainOnlyHandler = new ValveFluidHandler(false, true);
    private final IFluidHandler closedHandler = new ValveFluidHandler(false, false);

    @Nullable
    private ScrollOptionBehaviour<DrainSource> drainSource;

    private int burn;
    private int purity;
    private double rate;
    private double rateAccumulator;

    private boolean syncPending;
    private long nextSyncTick;

    public ReactorFluidValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        boolean input = state.getBlock() instanceof ReactorFluidValveBlock valve && valve.isInput();
        moltenTank = new FluidTank(input ? CAPACITY : OUTPUT_MOLTEN_CAPACITY, s -> ReactorFuel.isInput(s.getFluid()));
        heatedTank = input ? null : new FluidTank(OUTPUT_HEATED_CAPACITY, s -> ReactorFuel.isOutput(s.getFluid()));
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (isInput()) return;
        drainSource = new ScrollOptionBehaviour<>(DrainSource.class,
                Component.translatable("createnuclearized.salt_valve.drain_source"), this,
                new CenteredSideValueBoxTransform((state, dir) -> true));
        behaviours.add(drainSource);
    }

    public boolean isInput() {
        return getBlockState().getBlock() instanceof ReactorFluidValveBlock valve && valve.isInput();
    }

    @Nullable
    public DrainSource getDrainSource() {
        return drainSource == null ? null : drainSource.get();
    }

    public void setDrainSource(DrainSource source) {
        if (drainSource != null)
            drainSource.setValue(source.ordinal());
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (isInput()) return fluidHandler;
        if (side == Direction.UP) return fillOnlyHandler;
        if (side == null || side == Direction.DOWN) return drainOnlyHandler;
        return closedHandler;
    }

    public FluidTank getMoltenTank() {
        return moltenTank;
    }

    public int getHeatedCapacity() {
        return heatedTank == null ? 0 : heatedTank.getCapacity();
    }

    public int getMoltenAmount() {
        return moltenTank.getFluidAmount();
    }

    public int getHeatedAmount() {
        return heatedTank == null ? 0 : heatedTank.getFluidAmount();
    }

    public int fillHeated(FluidStack stack, FluidAction action) {
        return heatedTank == null ? 0 : heatedTank.fill(stack, action);
    }

    private FluidStack heatedFluid() {
        return heatedTank == null ? FluidStack.EMPTY : heatedTank.getFluid();
    }

    private FluidTank drainTank() {
        if (heatedTank == null) return moltenTank;
        return drainSource != null && drainSource.get() == DrainSource.INPUT ? moltenTank : heatedTank;
    }

    @Nullable
    private FluidTank tank(int index) {
        return switch (index) {
            case 0 -> moltenTank;
            case 1 -> heatedTank;
            default -> null;
        };
    }

    public void setStats(int burn, int purity, double rate) {
        boolean changed = this.burn != burn || this.purity != purity || this.rate != rate;
        this.burn = burn;
        this.purity = purity;
        this.rate = rate;
        if (changed)
            notifyContentsChanged();
    }

    public void clearStats() {
        rateAccumulator = 0;
        setStats(0, 0, 0);
    }

    public boolean hasStats() {
        return burn != 0 || purity != 0 || rate != 0;
    }

    public void advanceRate() {
        rateAccumulator = Math.min(RATE_ACCUM_CAP, rateAccumulator + rate);
    }

    public int pendingWholeMb() {
        return (int) rateAccumulator;
    }

    public void consumePending(int mb) {
        rateAccumulator = Math.max(0, rateAccumulator - mb);
    }

    public void notifyContentsChanged() {
        setChanged();
        syncPending = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !syncPending || level.getGameTime() < nextSyncTick) return;
        syncPending = false;
        nextSyncTick = level.getGameTime() + SYNC_INTERVAL;
        sendData();
    }

    private class ValveFluidHandler implements IFluidHandler {

        private final boolean fills;
        private final boolean drains;

        ValveFluidHandler(boolean fills, boolean drains) {
            this.fills = fills;
            this.drains = drains;
        }

        @Override
        public int getTanks() {
            return heatedTank == null ? 1 : 2;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            FluidTank tank = tank(index);
            return tank == null ? FluidStack.EMPTY : tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            FluidTank tank = tank(index);
            return tank == null ? 0 : tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return fills && tank == 0 && ReactorFuel.isInput(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!fills || !ReactorFuel.isInput(resource.getFluid())) return 0;
            int filled = moltenTank.fill(resource, action);
            if (filled > 0 && action.execute())
                notifyContentsChanged();
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidTank tank = drainTank();
            if (!drains || !FluidStack.isSameFluidSameComponents(resource, tank.getFluid())) return FluidStack.EMPTY;
            return doDrain(tank, resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!drains) return FluidStack.EMPTY;
            return doDrain(drainTank(), maxDrain, action);
        }

        private FluidStack doDrain(FluidTank tank, int amount, FluidAction action) {
            FluidStack drained = tank.drain(amount, action);
            if (!drained.isEmpty() && action.execute())
                notifyContentsChanged();
            return drained;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidHandler);

        ReactorFuel fuel = displayFuel();
        SaltStats stats = heatedFluid().get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            SaltStatsTooltip.append(tooltip, stats, fuel);
        if (!isInput() && hasStats())
            SaltStatsTooltip.appendChannel(tooltip, burn, purity, rate, fuel);
        return true;
    }

    @Nullable
    ReactorFuel displayFuel() {
        ReactorFuel spent = ReactorFuel.forOutput(heatedFluid().getFluid());
        return spent != null ? spent : moltenFuel();
    }

    @Nullable
    ReactorFuel moltenFuel() {
        return ReactorFuel.forInput(moltenTank.getFluid().getFluid());
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("MoltenTank", moltenTank.writeToNBT(registries, new CompoundTag()));
        if (heatedTank != null)
            tag.put("HeatedTank", heatedTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Burn", burn);
        tag.putInt("Purity", purity);
        tag.putDouble("Rate", rate);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("MoltenTank"))
            moltenTank.readFromNBT(registries, tag.getCompound("MoltenTank"));
        if (heatedTank != null && tag.contains("HeatedTank"))
            heatedTank.readFromNBT(registries, tag.getCompound("HeatedTank"));
        burn = tag.getInt("Burn");
        purity = tag.getInt("Purity");
        rate = tag.getDouble("Rate");
    }
}
