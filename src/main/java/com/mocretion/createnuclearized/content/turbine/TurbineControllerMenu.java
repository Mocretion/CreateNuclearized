package com.mocretion.createnuclearized.content.turbine;

import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.addWideInt;
import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.syncedInt;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.menu.ControllerMenu;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;

public class TurbineControllerMenu extends ControllerMenu {

    private int stages;
    private int steamBuffered;
    private int steamCapacity;
    private int steamUse;
    private int outputPercent;
    private int generatedRpm;

    public TurbineControllerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, null);
    }

    public TurbineControllerMenu(int containerId, Inventory playerInventory, TurbineControllerBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be);
    }

    private TurbineControllerMenu(int containerId, Inventory playerInventory, BlockPos pos,
                                  @Nullable TurbineControllerBlockEntity be) {
        super(CNMenuTypes.TURBINE_CONTROLLER.get(), containerId, playerInventory, pos, be, CNBlocks.TURBINE_CONTROLLER.get());
        addDataSlot(syncedInt(() -> be.getStages(), v -> stages = v));
        addWideInt(this::addDataSlot, () -> be.getSteamBuffered(), v -> steamBuffered = v);
        addWideInt(this::addDataSlot, () -> be.getSteamCapacity(), v -> steamCapacity = v);
        addDataSlot(syncedInt(() -> be.getSteamUse(), v -> steamUse = v));
        addDataSlot(syncedInt(() -> be.getOutputPercent(), v -> outputPercent = v));
        addDataSlot(syncedInt(() -> be.getGeneratedRpm(), v -> generatedRpm = v));
    }

    public int getStages() {
        return stages;
    }

    public int getSteamBuffered() {
        return steamBuffered;
    }

    public int getSteamCapacity() {
        return steamCapacity;
    }

    public int getSteamUse() {
        return steamUse;
    }

    public int getOutputPercent() {
        return outputPercent;
    }

    public int getGeneratedRpm() {
        return generatedRpm;
    }
}
