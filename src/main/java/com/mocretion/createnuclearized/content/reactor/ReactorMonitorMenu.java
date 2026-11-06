package com.mocretion.createnuclearized.content.reactor;

import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.addWideInt;
import static com.mocretion.createnuclearized.content.menu.MenuDataSlots.syncedInt;

import java.util.Map;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.menu.PlayerInventorySlots;
import com.mocretion.createnuclearized.content.multiblock.MultiblockState;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class ReactorMonitorMenu extends AbstractContainerMenu {

    private static final int INV_X = 8;
    private static final int INV_Y = 127;

    private final ContainerLevelAccess access;
    private final BlockPos pos;

    private boolean inventoryVisible = true;

    private int fissileBuffered;
    private int fissileCapacity;
    private int spentBuffered;
    private int spentCapacity;
    private int runSeconds;
    private int stateOrdinal;
    private int activeChannels;
    private int totalChannels;
    private int totalRateMilli;
    private int statHeat;
    private int statUnconsumed;
    private int statBred;
    private int statOffGas;
    private int fuelIndex;

    public ReactorMonitorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, null);
    }

    public ReactorMonitorMenu(int containerId, Inventory playerInventory, ReactorMonitorBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be);
    }

    private ReactorMonitorMenu(int containerId, Inventory playerInventory, BlockPos pos,
                               @Nullable ReactorMonitorBlockEntity be) {
        super(CNMenuTypes.REACTOR_MONITOR.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.pos = pos;

        PlayerInventorySlots.add(this::addSlot, playerInventory, INV_X, INV_Y, () -> inventoryVisible);

        addWideInt(this::addDataSlot, () -> be.getFissileBuffered(), v -> fissileBuffered = v);
        addWideInt(this::addDataSlot, () -> be.getFissileCapacity(), v -> fissileCapacity = v);
        addWideInt(this::addDataSlot, () -> be.getHeatedBuffered(), v -> spentBuffered = v);
        addWideInt(this::addDataSlot, () -> be.getSpentCapacity(), v -> spentCapacity = v);
        addWideInt(this::addDataSlot, () -> be.getRunSeconds(), v -> runSeconds = v);
        addDataSlot(syncedInt(() -> be.getReactorStateOrdinal(), v -> stateOrdinal = v));
        addDataSlot(syncedInt(() -> be.getActiveChannels(), v -> activeChannels = v));
        addDataSlot(syncedInt(() -> be.getTotalChannels(), v -> totalChannels = v));
        addWideInt(this::addDataSlot, () -> be.getTotalRateMilli(), v -> totalRateMilli = v);
        addWideInt(this::addDataSlot, () -> {
            SaltStats stats = be.getOutputStats();
            return stats == null ? 0 : stats.heat();
        }, v -> statHeat = v);
        addDataSlot(syncedInt(() -> elementAmount(be, unconsumedOf(be)), v -> statUnconsumed = v));
        addDataSlot(syncedInt(() -> elementAmount(be, bredOf(be)), v -> statBred = v));
        addDataSlot(syncedInt(() -> {
            SaltStats stats = be.getOutputStats();
            return stats == null ? 0 : stats.offGas();
        }, v -> statOffGas = v));
        addDataSlot(syncedInt(() -> {
            ReactorFuel current = be.getFuel();
            return current == null ? -1 : current.index();
        }, v -> fuelIndex = v));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getFissileBuffered() {
        return fissileBuffered;
    }

    public int getFissileCapacity() {
        return fissileCapacity;
    }

    public int getSpentBuffered() {
        return spentBuffered;
    }

    public int getSpentCapacity() {
        return spentCapacity;
    }

    public int getRunSeconds() {
        return runSeconds;
    }

    public MultiblockState getState() {
        return MultiblockState.byOrdinal(stateOrdinal);
    }

    public int getActiveChannels() {
        return activeChannels;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public double getTotalRate() {
        return totalRateMilli / 1000.0;
    }

    public SaltStats getOutputStats() {
        ReactorFuel fuel = getFuel();
        SaltStats stats = new SaltStats(statHeat, Map.of(), statOffGas);
        if (fuel == null) return stats;
        if (fuel.unconsumed() != null)
            stats = stats.withElement(fuel.unconsumed(), statUnconsumed);
        if (fuel.bred() != null && fuel.bred() != fuel.unconsumed())
            stats = stats.withElement(fuel.bred(), statBred);
        return stats;
    }

    @Nullable
    public ReactorFuel getFuel() {
        return fuelIndex < 0 ? null : ReactorFuel.byIndex(fuelIndex);
    }

    public void setInventoryVisible(boolean visible) {
        this.inventoryVisible = visible;
    }

    private static int elementAmount(ReactorMonitorBlockEntity be, @Nullable SaltElement element) {
        if (element == null) return 0;
        SaltStats stats = be.getOutputStats();
        return stats == null ? 0 : stats.amount(element);
    }

    @Nullable
    private static SaltElement unconsumedOf(ReactorMonitorBlockEntity be) {
        ReactorFuel fuel = be.getFuel();
        return fuel == null ? null : fuel.unconsumed();
    }

    @Nullable
    private static SaltElement bredOf(ReactorMonitorBlockEntity be) {
        ReactorFuel fuel = be.getFuel();
        return fuel == null ? null : fuel.bred();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, CNBlocks.REACTOR_MONITOR.get());
    }
}
