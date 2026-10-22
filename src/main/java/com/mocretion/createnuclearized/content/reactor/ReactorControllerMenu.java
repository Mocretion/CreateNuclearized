package com.mocretion.createnuclearized.content.reactor;

import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.addWideInt;
import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.syncedInt;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.menu.ControllerMenu;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;

public class ReactorControllerMenu extends ControllerMenu {

    private int fissileCapacity;
    private int spentCapacity;
    private int totalChannels;

    public ReactorControllerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, null);
    }

    public ReactorControllerMenu(int containerId, Inventory playerInventory, ReactorControllerBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be);
    }

    private ReactorControllerMenu(int containerId, Inventory playerInventory, BlockPos pos,
                                  @Nullable ReactorControllerBlockEntity be) {
        super(CNMenuTypes.REACTOR_CONTROLLER.get(), containerId, playerInventory, pos, be, CNBlocks.REACTOR_CONTROLLER.get());
        addWideInt(this::addDataSlot, () -> be.getFissileCapacity(), v -> fissileCapacity = v);
        addWideInt(this::addDataSlot, () -> be.getSpentCapacity(), v -> spentCapacity = v);
        addDataSlot(syncedInt(() -> be.getTotalChannels(), v -> totalChannels = v));
    }

    public int getFissileCapacity() {
        return fissileCapacity;
    }

    public int getSpentCapacity() {
        return spentCapacity;
    }

    public int getTotalChannels() {
        return totalChannels;
    }
}
