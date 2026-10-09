package com.mocretion.createnuclearized.content.turbine;

import java.util.List;

import com.mocretion.createnuclearized.registry.CNFluids;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class SteamInputValveBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 4000;

    private final FluidTank steamTank = new FluidTank(CAPACITY, s -> s.getFluid() == CNFluids.pressurizedSteam());
    private final IFluidHandler fluidHandler = new SteamFluidHandler();

    private long lastSyncTick = Long.MIN_VALUE;

    public SteamInputValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public int getSteamAmount() {
        return steamTank.getFluidAmount();
    }

    public int consume(int max) {
        FluidStack drained = steamTank.drain(max, FluidAction.EXECUTE);
        if (!drained.isEmpty())
            notifyContentsChanged();
        return drained.getAmount();
    }

    public void notifyContentsChanged() {
        setChanged();
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        if (now == lastSyncTick) return;
        lastSyncTick = now;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private class SteamFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return steamTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return steamTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid() == CNFluids.pressurizedSteam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != CNFluids.pressurizedSteam()) return 0;
            int filled = steamTank.fill(resource, action);
            if (filled > 0 && action.execute())
                notifyContentsChanged();
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != steamTank.getFluid().getFluid()) return FluidStack.EMPTY;
            return doDrain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return doDrain(maxDrain, action);
        }

        private FluidStack doDrain(int amount, FluidAction action) {
            FluidStack drained = steamTank.drain(amount, action);
            if (!drained.isEmpty() && action.execute())
                notifyContentsChanged();
            return drained;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidHandler);
        return true;
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
        tag.put("SteamTank", steamTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SteamTank"))
            steamTank.readFromNBT(registries, tag.getCompound("SteamTank"));
    }
}
