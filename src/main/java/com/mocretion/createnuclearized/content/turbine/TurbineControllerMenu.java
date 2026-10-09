package com.mocretion.createnuclearized.content.turbine;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

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

public class TurbineControllerMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE = 0;

    private final ContainerLevelAccess access;
    private final Block block;
    private final TurbineControllerBlockEntity be;

    private int stages;
    private int bladeCells;
    private int steamBuffered;
    private int steamCapacity;
    private int steamUse;
    private int outputPercent;
    private int generatedRpm;

    public TurbineControllerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                (TurbineControllerBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    public TurbineControllerMenu(int containerId, Inventory playerInventory, TurbineControllerBlockEntity be) {
        super(CNMenuTypes.TURBINE_CONTROLLER.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.block = be.getBlockState().getBlock();

        int invX = 8;
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, invX + col * 18, invY + 58));
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return be.isRunning() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                be.setRunningFromNetwork(value != 0);
            }
        });
        addDataSlot(syncedInt(be::getStateOrdinal, be::setStateFromNetwork));
        addDataSlot(syncedInt(be::getStages, v -> stages = v));
        addDataSlot(syncedInt(be::getBladeCells, v -> bladeCells = v));
        addWideInt(be::getSteamBuffered, v -> steamBuffered = v);
        addWideInt(be::getSteamCapacity, v -> steamCapacity = v);
        addDataSlot(syncedInt(be::getSteamUse, v -> steamUse = v));
        addDataSlot(syncedInt(be::getOutputPercent, v -> outputPercent = v));
        addDataSlot(syncedInt(be::getGeneratedRpm, v -> generatedRpm = v));
    }

    private static DataSlot syncedInt(IntSupplier getter, IntConsumer setter) {
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
    private void addWideInt(IntSupplier getter, IntConsumer setter) {
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

    public TurbineState getState() {
        return be.getState();
    }

    public boolean isRunning() {
        return be.isRunning();
    }

    public int getStages() {
        return stages;
    }

    public int getBladeCells() {
        return bladeCells;
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE && !player.level().isClientSide) {
            be.toggleRunning(player);
            return true;
        }
        return false;
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
