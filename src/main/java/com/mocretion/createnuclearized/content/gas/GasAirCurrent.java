package com.mocretion.createnuclearized.content.gas;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

public interface GasAirCurrent {

    ThreadLocal<GasAirCurrent> TICKING = new ThreadLocal<>();

    @Nullable
    BlockPos createnuclearized$gasBasin();

    void createnuclearized$setGasBasin(@Nullable BlockPos pos);

    int createnuclearized$gasColor();
}
