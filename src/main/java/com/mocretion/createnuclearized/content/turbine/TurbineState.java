package com.mocretion.createnuclearized.content.turbine;

public enum TurbineState {

    STOPPED("Stopped", 0xC0392B),  // Color used in UI
    IDLE("Idle", 0xB8860B),
    RUNNING("Running", 0x2E8B57);

    private final String label;
    private final int color;

    TurbineState(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }

    public static TurbineState byOrdinal(int ordinal) {
        TurbineState[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
