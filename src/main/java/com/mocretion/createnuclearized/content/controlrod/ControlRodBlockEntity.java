package com.mocretion.createnuclearized.content.controlrod;

import com.mocretion.createnuclearized.content.reactor.ReactorMultiblock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ControlRodBlockEntity extends KineticBlockEntity {

    // Blocks moved per tick per RPM ~1.25 blocks/s at 32 RPM
    private static final float BLOCKS_PER_TICK_PER_RPM = 1f / 512f;
    // Hard cap on reach, guarding the downward scan
    private static final int MAX_REACH = 16;

    private float offset;                  // extension in blocks below the rod (0 = retracted)
    private float prevOffset;              // last ticks offset, for render interpolation
    private float maxExtension = MAX_REACH; // open space below, rescanned periodically; clamps offset

    public ControlRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void tick() {
        super.tick();
        prevOffset = offset;

        float speed = getSpeed();
        if (speed == 0) return;

        float next = Mth.clamp(offset + speed * BLOCKS_PER_TICK_PER_RPM, 0f, maxExtension);
        if (next != offset) {
            offset = next;
            if (level != null && !level.isClientSide)
                setChanged();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateMaxExtension();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        // Resync the current offset when the drive changes, so clients advance from the right base
        if (level != null && !level.isClientSide)
            sendData();
    }

    /** Scan straight down for open space, so the rod stops at the floor */
    private void updateMaxExtension() {
        if (level == null) return;
        int reach = 0;
        BlockPos.MutableBlockPos cursor = worldPosition.mutable();
        for (int i = 1; i <= MAX_REACH; i++) {
            cursor.set(worldPosition.getX(), worldPosition.getY() - i, worldPosition.getZ());
            // The interior plumbing (Create fluid pipes) counts as open space, so the rod slides down through a pipe channel rather than
            // stopping on top of it
            BlockState below = level.getBlockState(cursor);
            if (!below.isAir() && !ReactorMultiblock.isFluidConduit(below)) break;
            reach = i;
        }
        maxExtension = reach;
        if (offset > maxExtension)
            offset = maxExtension;
    }

    /**
     * The rod hangs up to MAX_REACH blocks down, so grow the render box downward (and a little up for the shaft nub); otherwise its culled
     * the moment the anchor block leaves the frustum. Same trick as Create's pulley
     */
    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox()
                .expandTowards(0, -MAX_REACH, 0)
                .expandTowards(0, 1, 0);
    }

    /** Interpolated extension for rendering, in blocks */
    public float getRenderedExtension(float partialTicks) {
        return Mth.lerp(partialTicks, prevOffset, offset);
    }

    public float getExtension() {
        return offset;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Offset", offset);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        offset = tag.getFloat("Offset");
        prevOffset = offset;
    }
}
