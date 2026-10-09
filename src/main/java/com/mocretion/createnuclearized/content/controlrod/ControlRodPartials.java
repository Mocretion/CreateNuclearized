package com.mocretion.createnuclearized.content.controlrod;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class ControlRodPartials {

    public static final PartialModel CONTROL_ROD_EXTENSION = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/control_rod_extension"));

    private ControlRodPartials() {}

    public static void init() {}
}
