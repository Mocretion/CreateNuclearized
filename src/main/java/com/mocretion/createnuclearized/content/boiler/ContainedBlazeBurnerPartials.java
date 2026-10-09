package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class ContainedBlazeBurnerPartials {

    public static final PartialModel HAZMAT_HELMET = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/contained_blaze_helmet"));

    private ContainedBlazeBurnerPartials() {}

    public static void init() {}
}
