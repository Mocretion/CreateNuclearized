package com.mocretion.createnuclearized.content.multiblock;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MultiblockErrorClient {

    private static final int DURATION_TICKS = 100;
    private static final int COLOR = 0xFF_ff5d6c;

    private MultiblockErrorClient() {}

    public static void show(BlockPos pos) {
        Outliner.getInstance()
                .showAABB(Pair.of("multiblock_error", pos), new AABB(pos), DURATION_TICKS)
                .colored(COLOR);
    }
}
