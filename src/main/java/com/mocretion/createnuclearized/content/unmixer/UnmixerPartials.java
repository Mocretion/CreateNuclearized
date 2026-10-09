package com.mocretion.createnuclearized.content.unmixer;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class UnmixerPartials {

    public static final PartialModel UNMIXER_POLE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/unmixer/pole"));

    public static final PartialModel UNMIXER_HEAD = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/unmixer/head"));

    private UnmixerPartials() {}

    public static void init() {}
}
