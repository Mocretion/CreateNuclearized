package com.mocretion.createnuclearized.content.reactor;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class ReactorFluidValveBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 2000;
    public static final int OUTPUT_MOLTEN_CAPACITY = 100;
    public static final int OUTPUT_HEATED_CAPACITY = 500;

    private final FluidTank moltenTank;
    private final FluidTank heatedTank;
    private final IFluidHandler fluidHandler = new ValveFluidHandler();

    private long lastSyncTick = Long.MIN_VALUE;

    private static final double RATE_ACCUM_CAP = 10.0;

    private int burn;
    private int purity;
    private double rate;
    private double rateAccumulator;

    public ReactorFluidValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        boolean input = state.getBlock() instanceof ReactorFluidValveBlock valve && valve.isInput();
        int moltenCap = input ? CAPACITY : OUTPUT_MOLTEN_CAPACITY;
        int heatedCap = input ? CAPACITY : OUTPUT_HEATED_CAPACITY;
        moltenTank = new FluidTank(moltenCap, s -> ReactorFuel.isInput(s.getFluid()));
        heatedTank = new FluidTank(heatedCap, s -> ReactorFuel.isOutput(s.getFluid()));
    }

    public boolean isInput() {
        return getBlockState().getBlock() instanceof ReactorFluidValveBlock valve && valve.isInput();
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public FluidTank getMoltenTank() {
        return moltenTank;
    }

    public FluidTank getHeatedTank() {
        return heatedTank;
    }

    public int getMoltenAmount() {
        return moltenTank.getFluidAmount();
    }

    public int getHeatedAmount() {
        return heatedTank.getFluidAmount();
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

    public double getRate() {
        return rate;
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
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        if (now == lastSyncTick) return;
        lastSyncTick = now;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private class ValveFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return (tank == 0 ? moltenTank : heatedTank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return (tank == 0 ? moltenTank : heatedTank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && ReactorFuel.isInput(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!ReactorFuel.isInput(resource.getFluid())) return 0;
            int filled = moltenTank.fill(resource, action);
            if (filled > 0 && action.execute())
                notifyContentsChanged();
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidTank tank = isInput() ? moltenTank : heatedTank;
            if (!FluidStack.isSameFluidSameComponents(resource, tank.getFluid())) return FluidStack.EMPTY;
            return doDrain(tank, resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return doDrain(isInput() ? moltenTank : heatedTank, maxDrain, action);
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
        SaltStats stats = heatedTank.getFluid().get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            SaltStatsTooltip.append(tooltip, stats, fuel);
        if (!isInput() && hasStats())
            SaltStatsTooltip.appendChannel(tooltip, burn, purity, rate, fuel);
        return true;
    }

    @Nullable
    ReactorFuel displayFuel() {
        ReactorFuel spent = ReactorFuel.forOutput(heatedTank.getFluid().getFluid());
        return spent != null ? spent : moltenFuel();
    }

    @Nullable
    ReactorFuel moltenFuel() {
        return ReactorFuel.forInput(moltenTank.getFluid().getFluid());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null)
            loadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("MoltenTank", moltenTank.writeToNBT(registries, new CompoundTag()));
        tag.put("HeatedTank", heatedTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Burn", burn);
        tag.putInt("Purity", purity);
        tag.putDouble("Rate", rate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MoltenTank"))
            moltenTank.readFromNBT(registries, tag.getCompound("MoltenTank"));
        if (tag.contains("HeatedTank"))
            heatedTank.readFromNBT(registries, tag.getCompound("HeatedTank"));
        burn = tag.getInt("Burn");
        purity = tag.getInt("Purity");
        rate = tag.getDouble("Rate");
    }
}
