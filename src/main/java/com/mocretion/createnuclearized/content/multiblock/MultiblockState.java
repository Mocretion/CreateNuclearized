package com.mocretion.createnuclearized.content.multiblock;

import java.util.Locale;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum MultiblockState implements StringRepresentable {

    STOPPED(0xC0392B),
    IDLE(0xB8860B),
    RUNNING(0x2E8B57);

    private final int color;

    MultiblockState(int color) {
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component label() {
        return Component.translatable(CreateNuclearized.MODID + ".multiblock.state." + getSerializedName());
    }

    public int color() {
        return color;
    }

    public static MultiblockState byOrdinal(int ordinal) {
        MultiblockState[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
