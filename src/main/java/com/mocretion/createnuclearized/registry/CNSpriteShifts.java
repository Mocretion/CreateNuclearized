package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

import net.minecraft.resources.ResourceLocation;

public final class CNSpriteShifts {

    public static final CTSpriteShiftEntry REACTOR_CASING = omni("reactor_casing");
    public static final CTSpriteShiftEntry REACTOR_GLASS = omni("reactor_glass");

    private CNSpriteShifts() {}

    /** block/<name> -> block/<name>_connected, casing style omnidirectional */
    private static CTSpriteShiftEntry omni(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/" + name),
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/" + name + "_connected"));
    }
}
