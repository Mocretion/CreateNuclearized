package com.mocretion.createnuclearized.content.menu;

import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.syncedInt;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.multiblock.MultiblockController;
import com.mocretion.createnuclearized.content.multiblock.MultiblockState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public abstract class ControllerMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE = 0;

    private static final int INV_X = 8;
    private static final int INV_Y = 94;

    private final ContainerLevelAccess access;
    private final Block block;
    @Nullable
    private final MultiblockController controller;

    private boolean running;
    private int stateOrdinal;

    protected ControllerMenu(MenuType<?> type, int containerId, Inventory playerInventory, BlockPos pos,
                             @Nullable MultiblockController controller, Block block) {
        super(type, containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.block = block;
        this.controller = controller;

        PlayerInventorySlots.add(this::addSlot, playerInventory, INV_X, INV_Y);
        addDataSlot(syncedInt(() -> controller.isRunning() ? 1 : 0, v -> running = v != 0));
        addDataSlot(syncedInt(() -> controller.getState().ordinal(), v -> stateOrdinal = v));
    }

    public boolean isRunning() {
        return running;
    }

    public MultiblockState getState() {
        return MultiblockState.byOrdinal(stateOrdinal);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE || controller == null) return false;
        controller.toggleRunning(player);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }
}
