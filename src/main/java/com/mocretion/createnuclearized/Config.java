package com.mocretion.createnuclearized;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // No config options yet; the spec is still registered so the Mods screen has a config entry to grow into
    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
