package com.mocretion.createnuclearized.content.menu;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.world.inventory.DataSlot;

public final class MenuDataSlots {

    private MenuDataSlots() {}

    public static DataSlot syncedInt(IntSupplier getter, IntConsumer setter) {
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
    public static void addWideInt(Consumer<DataSlot> slots, IntSupplier getter, IntConsumer setter) {
        int[] parts = new int[2];
        slots.accept(new DataSlot() {
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
        slots.accept(new DataSlot() {
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
}
