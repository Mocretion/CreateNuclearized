package com.mocretion.createnuclearized.content.reactor;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

public enum ReactorState implements StringRepresentable {

    STOPPED("Stopped", 0xC0392B),
    IDLE("Idle", 0xB8860B),
    RUNNING("Running", 0x2E8B57);

    private final String label;
    private final int color;

    ReactorState(String label, int color) {
        this.label = label;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }

    public static ReactorState byOrdinal(int ordinal) {
        ReactorState[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
