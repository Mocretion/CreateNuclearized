package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorMonitorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int RESOLVE_INTERVAL = 20;

    @Nullable
    private BlockPos controllerCache;
    private long lastResolveTick = -RESOLVE_INTERVAL;

    public ReactorMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public ReactorControllerBlockEntity getController() {
        if (level == null) return null;
        if (controllerCache != null && level.getBlockEntity(controllerCache) instanceof ReactorControllerBlockEntity cached) {
            return cached;
        }
        controllerCache = null;

        long now = level.getGameTime();
        if (now - lastResolveTick < RESOLVE_INTERVAL) return null;
        lastResolveTick = now;

        ReactorMultiblock.Result result = ReactorMultiblock.validate(level, worldPosition);
        if (!result.valid()) return null;
        BlockPos controllerPos = result.structure().controller();
        if (level.getBlockEntity(controllerPos) instanceof ReactorControllerBlockEntity controller) {
            controllerCache = controllerPos;
            return controller;
        }
        return null;
    }

    public int getHeatedBuffered() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getHeatedBuffered();
    }

    public int getFissileBuffered() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getFissileBuffered();
    }

    public int getFissileCapacity() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getFissileCapacity();
    }

    public int getSpentCapacity() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getSpentCapacity();
    }

    public int getRunSeconds() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getRunSeconds();
    }

    public int getReactorStateOrdinal() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? ReactorState.STOPPED.ordinal() : controller.getStateOrdinal();
    }

    public int getActiveChannels() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getActiveChannels();
    }

    public int getTotalChannels() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getTotalChannels();
    }

    public int getTotalRateMilli() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getTotalRateMilli();
    }

    public SaltStats getOutputStats() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? SaltStats.EMPTY : controller.getOutputStats();
    }

    public int getFuelOrdinal() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? ReactorFuel.URANIUM.ordinal() : controller.getFuel().ordinal();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorMonitorMenu(containerId, playerInventory, this);
    }
}
