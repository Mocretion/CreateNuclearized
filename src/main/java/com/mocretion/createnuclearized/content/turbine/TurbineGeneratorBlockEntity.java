package com.mocretion.createnuclearized.content.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineGeneratorBlockEntity extends GeneratingKineticBlockEntity {

    private static final float SPEED_RPM = 128f;
    private static final int DRIVE_TIMEOUT = 10;

    private float stressCapacity;
    private int driveTimer = DRIVE_TIMEOUT;

    public TurbineGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getGeneratedSpeed() {
        return stressCapacity > 0 ? SPEED_RPM : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        this.lastCapacityProvided = stressCapacity;
        return stressCapacity;
    }

    public void drive(float stressCapacity) {
        driveTimer = DRIVE_TIMEOUT;
        setStressCapacity(stressCapacity);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (driveTimer > 0)
            driveTimer--;
        else
            setStressCapacity(0);
    }

    private void setStressCapacity(float stressCapacity) {
        if (Mth.equal(this.stressCapacity, stressCapacity)) return;
        this.stressCapacity = stressCapacity;
        updateGeneratedRotation();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putFloat("StressCapacity", stressCapacity);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        stressCapacity = compound.getFloat("StressCapacity");
        super.read(compound, registries, clientPacket);
    }
}
