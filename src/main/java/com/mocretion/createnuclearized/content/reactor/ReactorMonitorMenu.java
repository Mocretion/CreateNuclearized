package com.mocretion.createnuclearized.content.reactor;

import com.mocretion.createnuclearized.registry.CNMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ReactorMonitorMenu extends AbstractContainerMenu {

    private static final int INV_X = 8;
    private static final int INV_Y = 109;

    private final ContainerLevelAccess access;
    private final Block block;
    private final ReactorMonitorBlockEntity be;

    // Client synced display values
    private int fissileBuffered;
    private int fissileCapacity;
    private int depletedBuffered;
    private int depletedCapacity;
    private int runSeconds;
    private int stateOrdinal;
    private int activeChannels;
    private int totalChannels;
    private int totalRateMilli;
    private int statHeat;
    private int statUranium;
    private int statPlutonium;
    private int statThorium;
    private int statByproducts;

    /** Client side: resolve the BE from the synced position */
    public ReactorMonitorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                (ReactorMonitorBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    /** Server side */
    public ReactorMonitorMenu(int containerId, Inventory playerInventory, ReactorMonitorBlockEntity be) {
        super(CNMenuTypes.REACTOR_MONITOR.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.block = be.getBlockState().getBlock();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * 18, INV_Y + 58));
        }

        addDataSlot(syncedInt(be::getFissileBuffered, v -> fissileBuffered = v));
        addDataSlot(syncedInt(be::getFissileCapacity, v -> fissileCapacity = v));
        addDataSlot(syncedInt(be::getHeatedBuffered, v -> depletedBuffered = v));
        addDataSlot(syncedInt(be::getDepletedCapacity, v -> depletedCapacity = v));
        addDataSlot(syncedInt(be::getRunSeconds, v -> runSeconds = v));
        addDataSlot(syncedInt(be::getReactorStateOrdinal, v -> stateOrdinal = v));
        addDataSlot(syncedInt(be::getActiveChannels, v -> activeChannels = v));
        addDataSlot(syncedInt(be::getTotalChannels, v -> totalChannels = v));
        addDataSlot(syncedInt(be::getTotalRateMilli, v -> totalRateMilli = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().heat(), v -> statHeat = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().uranium(), v -> statUranium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().plutonium(), v -> statPlutonium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().thorium(), v -> statThorium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().byproducts(), v -> statByproducts = v));
    }

    private static DataSlot syncedInt(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }

    public int getFissileBuffered() {
        return fissileBuffered;
    }

    public int getFissileCapacity() {
        return fissileCapacity;
    }

    public int getDepletedBuffered() {
        return depletedBuffered;
    }

    public int getDepletedCapacity() {
        return depletedCapacity;
    }

    public int getRunSeconds() {
        return runSeconds;
    }

    public ReactorState getState() {
        return ReactorState.byOrdinal(stateOrdinal);
    }

    public int getActiveChannels() {
        return activeChannels;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    /** Max flow across all channels, mB/tick */
    public double getTotalRate() {
        return totalRateMilli / 1000.0;
    }

    /** Heated salt composition, rebuilt from the synced ints */
    public SaltStats getOutputStats() {
        return new SaltStats(statHeat, statUranium, statPlutonium, statThorium, statByproducts);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Only player inventory slots exist; nowhere to quick move to
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }
}
