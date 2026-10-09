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
    private int statUranium;
    private int statPlutonium;
    private int statThorium;
    private int statAmericium;
    private int statOffGas;
    private int fuelOrdinal;

    public ReactorMonitorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                (ReactorMonitorBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

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

        addWideInt(be::getFissileBuffered, v -> fissileBuffered = v);
        addWideInt(be::getFissileCapacity, v -> fissileCapacity = v);
        addWideInt(be::getHeatedBuffered, v -> spentBuffered = v);
        addWideInt(be::getSpentCapacity, v -> spentCapacity = v);
        addWideInt(be::getRunSeconds, v -> runSeconds = v);
        addDataSlot(syncedInt(be::getReactorStateOrdinal, v -> stateOrdinal = v));
        addDataSlot(syncedInt(be::getActiveChannels, v -> activeChannels = v));
        addDataSlot(syncedInt(be::getTotalChannels, v -> totalChannels = v));
        addWideInt(be::getTotalRateMilli, v -> totalRateMilli = v);
        addDataSlot(syncedInt(() -> be.getOutputStats().heat(), v -> statHeat = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().uranium(), v -> statUranium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().plutonium(), v -> statPlutonium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().thorium(), v -> statThorium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().americium(), v -> statAmericium = v));
        addDataSlot(syncedInt(() -> be.getOutputStats().offGas(), v -> statOffGas = v));
        addDataSlot(syncedInt(be::getFuelOrdinal, v -> fuelOrdinal = v));
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

    /** A DataSlot only carries a signed short, so values past ~32k go across two slots */
    private void addWideInt(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        int[] parts = new int[2];
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt() & 0xFFFF;
            }

            @Override
            public void set(int value) {
                parts[0] = value & 0xFFFF;
                setter.accept(parts[1] << 16 | parts[0]);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt() >>> 16 & 0xFFFF;
            }

            @Override
            public void set(int value) {
                parts[1] = value & 0xFFFF;
                setter.accept(parts[1] << 16 | parts[0]);
            }
        });
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

    public ReactorState getState() {
        return ReactorState.byOrdinal(stateOrdinal);
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
        return new SaltStats(statHeat, statUranium, statPlutonium, statThorium, statAmericium, statOffGas);
    }

    public ReactorFuel getFuel() {
        return ReactorFuel.values()[fuelOrdinal];
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
