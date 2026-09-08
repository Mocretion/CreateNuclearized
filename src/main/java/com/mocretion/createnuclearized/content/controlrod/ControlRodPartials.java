package com.mocretion.createnuclearized.content.controlrod;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class ControlRodPartials {

    // The rod that slides down out of the block; hand authored model, stretched in Y
    public static final PartialModel CONTROL_ROD_EXTENSION = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/control_rod_extension"));

    private ControlRodPartials() {}

    /** No op that forces this class to load so the partials above register before baking */
    public static void init() {}
}
