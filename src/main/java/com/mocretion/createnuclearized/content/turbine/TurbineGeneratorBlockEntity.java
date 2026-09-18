package com.mocretion.createnuclearized.content.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineGeneratorBlockEntity extends GeneratingKineticBlockEntity {

    // Output speed while running Provisional, tuned later
    private static final float SPEED_RPM = 160f;
    // Stress capacity per rotor cell, so a bigger/longer turbine supplies more SU Provisional
    private static final float CAPACITY_PER_BLADE_CELL = 2f;

    // Rotor cells across the turbine, drives stress capacity
    private int bladeCells;
    // Started by the controller
    private boolean running;
    // Whether steam is being fed this moment; gates the speed without changing the network topology
    private boolean hasSteam;
    // Counts lazy ticks (every 10 game ticks) so the structure self check runs roughly every 3-4 seconds as a fallback
    private int shellCheckTimer;

    public TurbineGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getGeneratedSpeed() {
        if (!running || !hasSteam)
            return 0;
        return SPEED_RPM;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = bladeCells * CAPACITY_PER_BLADE_CELL;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    // Controller hooks

    /** Start generating. Aligns the generator axis down the turbine and reads the rotor count. Called by the controller on start */
    public void startTurbine() {
        if (running || level == null)
            return;
        TurbineMultiblock.Result result = TurbineMultiblock.validate(level, worldPosition);
        if (!result.valid())
            return;
        // Face the axis down the shaft so the internal shaft and rotors are on our network
        Axis axis = result.depthAxis();
        BlockState state = getBlockState();
        if (state.getValue(RotatedPillarKineticBlock.AXIS) != axis)
            level.setBlock(worldPosition, state.setValue(RotatedPillarKineticBlock.AXIS, axis), Block.UPDATE_ALL);
        bladeCells = result.bladeCells();
        running = true;
        setChanged();
        updateGeneratedRotation();
    }

    /** Stop generating. Called by the controller on stop or when the shape breaks */
    public void stopTurbine() {
        if (!running)
            return;
        running = false;
        setChanged();
        updateGeneratedRotation();
    }

    /** Gate the speed on steam. Only re resolves the network on a change */
    public void setHasSteam(boolean hasSteam) {
        if (this.hasSteam == hasSteam)
            return;
        this.hasSteam = hasSteam;
        if (running)
            updateGeneratedRotation();
    }

    // Self guard: stop if the turbine breaks (e.g. the controller is gone and cant tell us to stop)
    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide || !running)
            return;
        if (++shellCheckTimer < 7)
            return;
        shellCheckTimer = 0;
        if (!TurbineMultiblock.validate(level, worldPosition).valid())
            stopTurbine();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("BladeCells", bladeCells);
        compound.putBoolean("Running", running);
        compound.putBoolean("HasSteam", hasSteam);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        bladeCells = compound.getInt("BladeCells");
        running = compound.getBoolean("Running");
        hasSteam = compound.getBoolean("HasSteam");
        super.read(compound, registries, clientPacket);
    }
}
