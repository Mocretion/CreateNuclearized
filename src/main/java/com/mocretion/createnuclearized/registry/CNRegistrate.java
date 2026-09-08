package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

public final class CNRegistrate {

    // Create's kinetic stats tooltip factory, so kinetic blocks like the control rod get their "Stress Impact" line
    // KineticStats.create returns null for non kinetic items and mapNull turns that into the no op EMPTY modifier, so its safe on
    // everything. Has to be set in this field initializer, which runs before CNBlocks/CNItems (they depend on this class) register
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreateNuclearized.MODID)
            .setTooltipModifierFactory(item -> TooltipModifier.mapNull(KineticStats.create(item)));

    private CNRegistrate() {}

    /**
     * Lang keys that arent tied to a registry entry (tab titles, config labels, tooltips)
     * Registrate generates the rest and writes the single en_us.json. Call once at mod construction
     */
    public static void registerRawLang() {
        REGISTRATE.addRawLang("itemGroup.createnuclearized", "Create: Nuclearized");

        REGISTRATE.addRawLang("createnuclearized.configuration.title", "Create: Nuclearized Configs");
        REGISTRATE.addRawLang("createnuclearized.configuration.section.createnuclearized.common.toml",
                "Create: Nuclearized Configs");
        REGISTRATE.addRawLang("createnuclearized.configuration.section.createnuclearized.common.toml.title",
                "Create: Nuclearized Configs");
        REGISTRATE.addRawLang("createnuclearized.configuration.items", "Item List");
        REGISTRATE.addRawLang("createnuclearized.configuration.logDirtBlock", "Log Dirt Block");
        REGISTRATE.addRawLang("createnuclearized.configuration.magicNumberIntroduction", "Magic Number Text");
        REGISTRATE.addRawLang("createnuclearized.configuration.magicNumber", "Magic Number");

        // Goggle overlay: heated salt composition (see ReactorFluidValveBlockEntity#addToGoggleTooltip)
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_stats", "Heated Salt Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_heat", "Heat");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_uranium", "Uranium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_plutonium", "Plutonium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_thorium", "Thorium");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_byproducts", "Byproducts");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_channel", "Channel Output");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_burn", "Burn");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_purity", "Purity");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_rate", "Rate");

        // Moderator / moderator cooler item tooltips (see ReactorTooltips)
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator", "Reactor Moderator");
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator.rate", "%s mB/t to each bridged channel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator.hint", "Sits between two channels");
        REGISTRATE.addRawLang("createnuclearized.tooltip.modifier", "Moderator Cooler");
        REGISTRATE.addRawLang("createnuclearized.tooltip.modifier.hint", "Place on a moderator's face");
        REGISTRATE.addRawLang("createnuclearized.tooltip.rate", "%s mB/t");
        REGISTRATE.addRawLang("createnuclearized.tooltip.delta.burn", "%s%% burn");
        REGISTRATE.addRawLang("createnuclearized.tooltip.delta.purity", "%s%% purity");
        // Shift expandable explanation of burn/purity, on every item that shows them
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.hint", "Hold Shift for burn & purity info");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.burn", "Burn: how much fuel reacts");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.burn.desc",
                "more heat & output, less leftover fuel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.purity", "Purity: quality of what reacts");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.purity.desc",
                "more useful material, fewer byproducts");
        // Element role tags shown after a composition line
        REGISTRATE.addRawLang("createnuclearized.tooltip.role.leftover", "(leftover)");
        REGISTRATE.addRawLang("createnuclearized.tooltip.role.generated", "(generated)");

        // Fuel salt bucket tooltips (base stats), see ReactorTooltips / ReactorFuel
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel", "Reactor Fuel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.heat", "Base heat: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.burn", "Base burn: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.purity", "Base purity: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.leftover", "Leftover: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.generated", "Generates: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.uranium", "Uranium");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.plutonium", "Plutonium");
        REGISTRATE.addRawLang("createnuclearized.tooltip.element.thorium", "Thorium");

        // Creative tab holding the reactors moderator and modifier blocks
        REGISTRATE.addRawLang("itemGroup.createnuclearized.components", "Create: Nuclearized Components");
    }
}
