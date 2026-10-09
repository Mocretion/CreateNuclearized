package com.mocretion.createnuclearized.content.radiation;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

public final class CNHeartTypes {

    public static final EnumProxy<Gui.HeartType> RADIOACTIVE = new EnumProxy<>(
            Gui.HeartType.class,
            heart("radioactive_full"),
            heart("radioactive_full_blinking"),
            heart("radioactive_half"),
            heart("radioactive_half_blinking"),
            heart("radioactive_hardcore_full"),
            heart("radioactive_hardcore_full_blinking"),
            heart("radioactive_hardcore_half"),
            heart("radioactive_hardcore_half_blinking"));

    private CNHeartTypes() {}

    public static Gui.HeartType radioactive() {
        return RADIOACTIVE.getValue();
    }

    private static ResourceLocation heart(String name) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "hud/heart/" + name);
    }
}
