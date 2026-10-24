package com.mocretion.createnuclearized.content.reactor;

import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.addWideInt;
import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.syncedInt;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.menu.ControllerMenu;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class ReactorControllerMenu extends ControllerMenu {

    public static final int BUTTON_TOGGLE_DRAIN = 1;

    @Nullable
    private final ReactorControllerBlockEntity reactor;

    private int fissileCapacity;
    private int spentCapacity;
    private int totalChannels;
    private int spentDrainCount;
    private int fuelDrainCount;

    public ReactorControllerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, null);
    }

    public ReactorControllerMenu(int containerId, Inventory playerInventory, ReactorControllerBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be);
    }

    private ReactorControllerMenu(int containerId, Inventory playerInventory, BlockPos pos,
                                  @Nullable ReactorControllerBlockEntity be) {
        super(CNMenuTypes.REACTOR_CONTROLLER.get(), containerId, playerInventory, pos, be, CNBlocks.REACTOR_CONTROLLER.get());
        this.reactor = be;
        addWideInt(this::addDataSlot, () -> be.getFissileCapacity(), v -> fissileCapacity = v);
        addWideInt(this::addDataSlot, () -> be.getSpentCapacity(), v -> spentCapacity = v);
        addDataSlot(syncedInt(() -> be.getTotalChannels(), v -> totalChannels = v));
        addDataSlot(syncedInt(() -> be.getSpentDrainCount(), v -> spentDrainCount = v));
        addDataSlot(syncedInt(() -> be.getFuelDrainCount(), v -> fuelDrainCount = v));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_DRAIN) {
            if (reactor != null)
                reactor.toggleDrainSources();
            return true;
        }
        return super.clickMenuButton(player, id);
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

    public int getSpentDrainCount() {
        return spentDrainCount;
    }

    public int getFuelDrainCount() {
        return fuelDrainCount;
    }
}
