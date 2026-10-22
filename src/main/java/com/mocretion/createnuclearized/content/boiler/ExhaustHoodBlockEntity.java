package com.mocretion.createnuclearized.content.boiler;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class ExhaustHoodBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 500;

    private SmartFluidTankBehaviour gasTank;

    public ExhaustHoodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        gasTank = SmartFluidTankBehaviour.single(this, CAPACITY).forbidInsertion();
        behaviours.add(gasTank);
    }

    public IFluidHandler getFluidHandler() {
        return gasTank.getCapability();
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;
        FluidStack gas = gasTank.getPrimaryHandler().getFluid();
        if (gas.isEmpty()) return;
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.above(), Direction.DOWN);
        if (tank == null) return;
        int filled = tank.fill(gas.copy(), FluidAction.EXECUTE);
        if (filled > 0)
            gasTank.getPrimaryHandler().drain(filled, FluidAction.EXECUTE);
    }

    public int addGas(FluidStack gas) {
        return gasTank.getPrimaryHandler().fill(gas, FluidAction.EXECUTE);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, getFluidHandler());
        return true;
    }
}
