package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

public final class CNRegistrate {

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateNuclearized.MODID)
            .defaultCreativeTab(CreateNuclearized.MAIN_TAB_KEY)
            .setTooltipModifierFactory(item -> TooltipModifier.mapNull(KineticStats.create(item)));

    private CNRegistrate() {}

    public static void registerRawLang() {
        REGISTRATE.addRawLang("itemGroup.createnuclearized", "Create: Nuclearized");

        REGISTRATE.addRawLang("createnuclearized.configuration.title", "Create: Nuclearized Configs");
        REGISTRATE.addRawLang("createnuclearized.configuration.section.createnuclearized.common.toml",
                "Create: Nuclearized Configs");
        REGISTRATE.addRawLang("createnuclearized.configuration.section.createnuclearized.common.toml.title",
                "Create: Nuclearized Configs");

        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_stats", "Heated Salt Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_heat", "Heat");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_uranium", "Uranium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_plutonium", "Plutonium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_thorium", "Thorium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_americium", "Americium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_off_gas", "Off-gas");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_channel", "Channel Output");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_burn", "Burn");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_purity", "Purity");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_rate", "Rate");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.basin_input", "Input Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.basin_output", "Output Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.basin_blocked", "Output blocked, drain it to continue");

        REGISTRATE.addRawLang("death.attack.createnuclearized.steam", "%1$s was scalded by steam");
        REGISTRATE.addRawLang("death.attack.createnuclearized.steam.player", "%1$s was scalded by steam while fighting %2$s");

        REGISTRATE.addRawLang("effect.createnuclearized.radiation_sickness", "Radiation Sickness");
        REGISTRATE.addRawLang("death.attack.createnuclearized.radiation", "%1$s succumbed to radiation sickness");
        REGISTRATE.addRawLang("death.attack.createnuclearized.radiation.player",
                "%1$s succumbed to radiation sickness while fighting %2$s");

        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator", "Reactor Moderator");
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator.rate", "%s mB/t to each bridged channel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator.hint", "Sits between two channels");
        REGISTRATE.addRawLang("createnuclearized.tooltip.modifier", "Moderator Cooler");
        REGISTRATE.addRawLang("createnuclearized.tooltip.modifier.hint", "Place on a moderator's face");
        REGISTRATE.addRawLang("createnuclearized.tooltip.rate", "%s mB/t");
        REGISTRATE.addRawLang("createnuclearized.tooltip.delta.burn", "%s%% burn");
        REGISTRATE.addRawLang("createnuclearized.tooltip.delta.purity", "%s%% purity");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.hint", "Hold Shift for burn & purity info");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.burn", "Burn: how much fuel reacts");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.burn.desc",
                "more heat & output, less unconsumed fuel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.purity", "Purity: quality of what reacts");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.purity.desc",
                "more useful material, less off-gas");
        REGISTRATE.addRawLang("createnuclearized.tooltip.precision_unmixer.flavor",
                "This machine does not obey the laws of physics at all");

        REGISTRATE.addRawLang("createnuclearized.tooltip.role.unconsumed", "(unconsumed)");
        REGISTRATE.addRawLang("createnuclearized.tooltip.role.bred", "(bred)");

        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel", "Reactor Fuel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.heat", "Base heat: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.burn", "Base burn: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.purity", "Base purity: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.unconsumed", "Unconsumed: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.bred", "Breeds: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.uranium", "Uranium");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.plutonium", "Plutonium");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.thorium", "Thorium");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.americium", "Americium");

        REGISTRATE.addRawLang("itemGroup.createnuclearized.components", "Create: Nuclearized Components");

        REGISTRATE.addRawLang("createnuclearized.turbine_rotor.no_space", "Clear the highlighted blocks to place the rotor");
    }
}
