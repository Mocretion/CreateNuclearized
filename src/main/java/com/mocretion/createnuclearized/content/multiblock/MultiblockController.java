package com.mocretion.createnuclearized.content.multiblock;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;

public interface MultiblockController {

    boolean isRunning();

    MultiblockState getState();

    void toggleRunning(@Nullable Player player);
}
