package com.mocretion.createnuclearized.content.menu;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class PlayerInventorySlots {

    private static final int SLOT_SIZE = 18;
    private static final int HOTBAR_OFFSET = 58;

    private PlayerInventorySlots() {}

    public static void add(Consumer<Slot> slots, Inventory inventory, int x, int y) {
        add(slots, inventory, x, y, () -> true);
    }

    public static void add(Consumer<Slot> slots, Inventory inventory, int x, int y, BooleanSupplier visible) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                slots.accept(slot(inventory, col + row * 9 + 9, x + col * SLOT_SIZE, y + row * SLOT_SIZE, visible));
        }
        for (int col = 0; col < 9; col++)
            slots.accept(slot(inventory, col, x + col * SLOT_SIZE, y + HOTBAR_OFFSET, visible));
    }

    private static Slot slot(Inventory inventory, int index, int x, int y, BooleanSupplier visible) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean isActive() {
                return visible.getAsBoolean();
            }
        };
    }
}
