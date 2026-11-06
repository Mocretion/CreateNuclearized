package com.mocretion.createnuclearized.content.boiler;

import java.util.Locale;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum CellSlot implements StringRepresentable {
    SINGLE(-1, -1, false),
    NW(0, 0, false), N(1, 0, false), NE(2, 0, false),
    W(0, 1, false), C(1, 1, false), E(2, 1, false),
    SW(0, 2, false), S(1, 2, false), SE(2, 2, false),
    SMALL_NW(0, 0, true), SMALL_NE(2, 0, true),
    SMALL_SW(0, 2, true), SMALL_SE(2, 2, true);

    public static final EnumProperty<CellSlot> PROPERTY = EnumProperty.create("slot", CellSlot.class);
    private static final int LAST = 2;

    public final int col;
    public final int row;
    public final boolean small;

    CellSlot(int col, int row, boolean small) {
        this.col = col;
        this.row = row;
        this.small = small;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isOuter(Direction side) {
        if (this == SINGLE) return side.getAxis().isHorizontal();
        return switch (side) {
            case NORTH -> row == 0;
            case SOUTH -> row == LAST;
            case WEST -> col == 0;
            case EAST -> col == LAST;
            default -> false;
        };
    }

    public CellSlot asLarge() {
        return switch (this) {
            case SMALL_NW -> NW;
            case SMALL_NE -> NE;
            case SMALL_SW -> SW;
            case SMALL_SE -> SE;
            default -> this;
        };
    }

    public static CellSlot of(int width, int dx, int dz) {
        if (width <= 1) return SINGLE;
        int last = width - 1;
        boolean n = dz == 0, s = dz == last, w = dx == 0, e = dx == last;
        if (width == 2) return n ? (w ? SMALL_NW : SMALL_NE) : (w ? SMALL_SW : SMALL_SE);
        if (n && w) return NW;
        if (n && e) return NE;
        if (n) return N;
        if (s && w) return SW;
        if (s && e) return SE;
        if (s) return S;
        if (w) return W;
        if (e) return E;
        return C;
    }
}
