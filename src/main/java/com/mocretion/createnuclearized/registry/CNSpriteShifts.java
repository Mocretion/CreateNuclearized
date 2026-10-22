package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

import net.minecraft.resources.ResourceLocation;

public final class CNSpriteShifts {

    public static final CTSpriteShiftEntry REACTOR_CASING = omni("reactor_casing");
    public static final CTSpriteShiftEntry REACTOR_GLASS = omni("reactor_glass");
    public static final CTSpriteShiftEntry TURBINE_CASING = omni("turbine_casing");
    public static final CTSpriteShiftEntry TURBINE_GLASS = omni("turbine_glass");

    public static final CTSpriteShiftEntry REACTOR_COGWHEEL_SIDE = cogSide(AllCTTypes.VERTICAL);
    public static final CTSpriteShiftEntry REACTOR_COGWHEEL_OTHERSIDE = cogSide(AllCTTypes.HORIZONTAL);

    private CNSpriteShifts() {}

    private static CTSpriteShiftEntry omni(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/" + name),
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/" + name + "_connected"));
    }

    private static CTSpriteShiftEntry cogSide(AllCTTypes type) {
        return CTSpriteShifter.getCT(type,
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/reactor_encased_cogwheel_side"),
                ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/reactor_encased_cogwheel_side_connected"));
    }
}
