package com.mocretion.createnuclearized.content.turbine;

import com.mocretion.createnuclearized.CreateNuclearized;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

public final class TurbineRotorPartials {

    // The spinning disc of each wheel (hub, sockets, root bolts, blades, shaft), indexed by rotor radius 1/2/3
    public static final PartialModel TURBINE_3 = block("turbines/turbine_3/rotor");
    public static final PartialModel TURBINE_5 = block("turbines/turbine_5/rotor");
    public static final PartialModel TURBINE_7 = block("turbines/turbine_7/rotor");

    // The stationary nacelle of each wheel (bearings, collars, hub bolts), drawn but never rotated
    public static final PartialModel TURBINE_3_STATIC = block("turbines/turbine_3/static");
    public static final PartialModel TURBINE_5_STATIC = block("turbines/turbine_5/static");
    public static final PartialModel TURBINE_7_STATIC = block("turbines/turbine_7/static");

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/" + path));
    }

    public static PartialModel forRadius(int radius) {
        return switch (radius) {
            case 2 -> TURBINE_5;
            case 3 -> TURBINE_7;
            default -> TURBINE_3;
        };
    }

    public static PartialModel forRadiusStatic(int radius) {
        return switch (radius) {
            case 2 -> TURBINE_5_STATIC;
            case 3 -> TURBINE_7_STATIC;
            default -> TURBINE_3_STATIC;
        };
    }

    private TurbineRotorPartials() {}

    /** No op that forces this class to load so the partials register before baking */
    public static void init() {}
}
