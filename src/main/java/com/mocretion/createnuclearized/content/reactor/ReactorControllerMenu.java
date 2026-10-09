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

public class ReactorControllerMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE = 0;

    private final ContainerLevelAccess access;
    private final Block block;
    private final ReactorControllerBlockEntity be;

    private int fissileCapacity;
    private int spentCapacity;
    private int totalChannels;

    public ReactorControllerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                (ReactorControllerBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    public ReactorControllerMenu(int containerId, Inventory playerInventory, ReactorControllerBlockEntity be) {
        super(CNMenuTypes.REACTOR_CONTROLLER.get(), containerId);
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

        addWideInt(be::getFissileCapacity, v -> fissileCapacity = v);
        addWideInt(be::getSpentCapacity, v -> spentCapacity = v);
        addDataSlot(syncedInt(be::getTotalChannels, v -> totalChannels = v));

        addDataSlot(syncedInt(be::getStateOrdinal, be::setStateFromNetwork));
    }

    public ReactorState getState() {
        return be.getState();
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

    public boolean isRunning() {
        return be.isRunning();
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
