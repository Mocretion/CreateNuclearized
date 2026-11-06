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

    private static final float BLOCKS_PER_TICK_PER_RPM = 1f / 512f;
    private static final int MAX_REACH = 16;
    private static final float DROP_PER_TICK = 0.25f;
    private static final int ENGAGE_TIMEOUT = 20;
    private static final float RAISED_EPS = 0.05f;

    private float offset = MAX_REACH;
    private float prevOffset = MAX_REACH;
    private float maxExtension = MAX_REACH;
    private int engagedTimer;
    private boolean engaged;

    public ControlRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateMaxExtension();
    }

    @Override
    public void tick() {
        super.tick();
        prevOffset = offset;
        if (level == null) return;
        boolean client = level.isClientSide;

        if (!client) {
            boolean nowEngaged = engagedTimer > 0;
            if (nowEngaged)
                engagedTimer--;
            if (nowEngaged != engaged) {
                engaged = nowEngaged;
                sendData();
            }
        }

        float next;
        float speed = Math.abs(getSpeed());
        if (speed > 0) {
            next = Mth.clamp(offset - speed * BLOCKS_PER_TICK_PER_RPM, 0f, maxExtension);
        } else if (engaged) {
            return;
        } else {
            if (offset >= maxExtension) return;
            next = Mth.clamp(offset + DROP_PER_TICK, 0f, maxExtension);
        }
        if (next != offset) {
            offset = next;
            if (!client)
                setChanged();
        }
    }

    public void engage() {
        engagedTimer = ENGAGE_TIMEOUT;
    }

    public boolean isRaised() {
        return maxExtension > 0 && offset <= RAISED_EPS;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateMaxExtension();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level != null && !level.isClientSide)
            sendData();
    }

    private void updateMaxExtension() {
        if (level == null) return;
        int reach = 0;
        BlockPos.MutableBlockPos cursor = worldPosition.mutable();
        for (int i = 1; i <= MAX_REACH; i++) {
            cursor.set(worldPosition.getX(), worldPosition.getY() - i, worldPosition.getZ());
            if (!ReactorMultiblock.isRodPassable(level.getBlockState(cursor))) break;
            reach = i;
        }
        maxExtension = reach;
        if (offset > maxExtension)
            offset = maxExtension;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox()
                .expandTowards(0, -MAX_REACH, 0)
                .expandTowards(0, 1, 0);
    }

    public float getRenderedExtension(float partialTicks) {
        return Mth.lerp(partialTicks, prevOffset, offset);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Offset", offset);
        tag.putBoolean("Engaged", engaged);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        offset = tag.contains("Offset") ? tag.getFloat("Offset") : MAX_REACH;
        prevOffset = offset;
        engaged = tag.getBoolean("Engaged");
        if (engaged && !clientPacket)
            engagedTimer = ENGAGE_TIMEOUT;
    }
}
