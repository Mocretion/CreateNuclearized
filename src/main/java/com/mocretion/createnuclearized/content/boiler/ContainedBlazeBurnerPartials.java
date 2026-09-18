package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class ContainedBlazeBurnerPartials {

    // The helmet the blaze wears; drawn as the blaze burners hat slot
    public static final PartialModel HAZMAT_HELMET = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/contained_blaze_helmet"));

    private ContainedBlazeBurnerPartials() {}

    /** No op that forces this class to load so the partial above registers before baking */
    public static void init() {}
}
