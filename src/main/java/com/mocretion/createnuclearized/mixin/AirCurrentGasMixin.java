package com.mocretion.createnuclearized.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mocretion.createnuclearized.content.boiler.EvaporationBasinBlockEntity;
import com.mocretion.createnuclearized.content.gas.GasAirCurrent;
import com.mocretion.createnuclearized.registry.CNFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

@Mixin(AirCurrent.class)
public abstract class AirCurrentGasMixin implements GasAirCurrent {

    @Shadow @Final public IAirCurrentSource source;
    @Shadow public Direction direction;
    @Shadow public boolean pushing;
    @Shadow public float maxDistance;

    @Unique
    private BlockPos createnuclearized$basinPos;
    @Unique
    private AABB createnuclearized$beamBounds;
    @Unique
    private int createnuclearized$crossingY;
    @Unique
    private int createnuclearized$gasColorValue = -1;

    @Override
    @Nullable
    public BlockPos createnuclearized$gasBasin() {
        return createnuclearized$basinPos;
    }

    @Override
    public void createnuclearized$setGasBasin(@Nullable BlockPos pos) {
        createnuclearized$basinPos = pos;
    }

    @Override
    public int createnuclearized$gasColor() {
        return createnuclearized$gasColorValue;
    }

    @Inject(method = "rebuild", at = @At("HEAD"))
    private void createnuclearized$resetGas(CallbackInfo ci) {
        createnuclearized$basinPos = null;
        createnuclearized$beamBounds = null;
        createnuclearized$gasColorValue = -1;
    }

    @Inject(method = "rebuild", at = @At("TAIL"))
    private void createnuclearized$detectGas(CallbackInfo ci) {
        Level world = source.getAirCurrentWorld();
        if (world == null || direction == null || maxDistance < 0.25f) return;

        BlockPos start = source.getAirCurrentPos();
        int limit = createnuclearized$limit();
        int from = pushing ? 1 : limit;
        int to = pushing ? limit : 1;
        int step = pushing ? 1 : -1;
        for (int i = from; step > 0 ? i <= to : i >= to; i += step) {
            BlockPos crossing = start.relative(direction, i);
            BlockPos basin = createnuclearized$basinUnder(world, crossing);
            if (basin == null) continue;
            BlockPos end = createnuclearized$gasSegmentEnd(world, start, i, limit, step);
            createnuclearized$basinPos = basin;
            createnuclearized$beamBounds = new AABB(crossing).minmax(new AABB(end)).inflate(0.25);
            createnuclearized$crossingY = crossing.getY();
            if (world.getBlockEntity(basin) instanceof EvaporationBasinBlockEntity be)
                createnuclearized$gasColorValue = be.getBeamColor();
            return;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void createnuclearized$beginTick(CallbackInfo ci) {
        GasAirCurrent.TICKING.set(this);
        if (createnuclearized$basinPos == null || createnuclearized$beamBounds == null) return;
        Level world = source.getAirCurrentWorld();
        if (world == null || world.isClientSide) return;
        if (world.getBlockEntity(createnuclearized$basinPos) instanceof EvaporationBasinBlockEntity basin && basin.isEmittingGas())
            basin.divert(createnuclearized$beamBounds, createnuclearized$crossingY);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void createnuclearized$endTick(CallbackInfo ci) {
        GasAirCurrent.TICKING.remove();
    }

    @Unique
    @Nullable
    private BlockPos createnuclearized$basinUnder(Level world, BlockPos cell) {
        EvaporationBasinBlockEntity basin = EvaporationBasinBlockEntity.emittingBasinBelow(world, cell);
        return basin == null ? null : basin.getBlockPos();
    }

    @Unique
    private BlockPos createnuclearized$gasSegmentEnd(Level world, BlockPos start, int crossingIndex, int limit, int step) {
        FanProcessingType gas = CNFanProcessingTypes.GAS.get();
        int from = pushing ? crossingIndex + step : crossingIndex;
        int to = pushing ? limit : 1;
        int last = crossingIndex;
        for (int j = from; step > 0 ? j <= to : j >= to; j += step) {
            FanProcessingType t = FanProcessingType.getAt(world, start.relative(direction, j));
            if (t != null && t != gas) break;
            last = j;
        }
        return start.relative(direction, last);
    }

    @Unique
    private int createnuclearized$limit() {
        return maxDistance == (float) (int) maxDistance ? (int) maxDistance : (int) maxDistance + 1;
    }
}
