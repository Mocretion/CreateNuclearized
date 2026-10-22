package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class IndustrialHeaterPartials {

    public static final PartialModel HAZMAT_HELMET = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/industrial_heater_helmet"));

    private IndustrialHeaterPartials() {}

    public static void init() {}
}
