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

        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_stats", "Heated Salt Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_heat", "Heat");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_off_gas", "Off-Gas");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_channel", "Channel Output");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_burn", "Burn");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_purity", "Purity");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.salt_rate", "Rate");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.basin_input", "Input Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.metering_dose", "Dose: %s mB");
        REGISTRATE.addRawLang("createnuclearized.metering_valve.dose", "Dose per Pulse");
        REGISTRATE.addRawLang("createnuclearized.metering_valve.fine", "1 mB steps");
        REGISTRATE.addRawLang("createnuclearized.metering_valve.coarse", "10 mB steps");
        REGISTRATE.addRawLang("createnuclearized.metering_valve.amount", "%s mB");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.basin_output", "Output Composition");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.basin_blocked", "Output blocked, drain it to continue");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.heater_buffers", "Heat Buffer");
        REGISTRATE.addRawLang("createnuclearized.generic.unit.heat", "heat");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.tank_input", "Input:");
        REGISTRATE.addRawLang("createnuclearized.gui.goggles.tank_output", "Output:");

        REGISTRATE.addRawLang("createnuclearized.salt_valve.drain_source", "Drain Source");
        REGISTRATE.addRawLang("createnuclearized.salt_valve.drain_source.output", "Spent Salt (Output)");
        REGISTRATE.addRawLang("createnuclearized.salt_valve.drain_source.input", "Fuel Salt (Input)");

        REGISTRATE.addRawLang("createnuclearized.industrial_heater.drain_source", "Drain Source");
        REGISTRATE.addRawLang("createnuclearized.industrial_heater.drain_source.output", "Cooled Fluid (Output)");
        REGISTRATE.addRawLang("createnuclearized.industrial_heater.drain_source.input", "Heater Fluid (Input)");

        REGISTRATE.addRawLang("death.attack.createnuclearized.steam", "%1$s was scalded by steam");
        REGISTRATE.addRawLang("death.attack.createnuclearized.steam.player", "%1$s was scalded by steam while fighting %2$s");

        REGISTRATE.addRawLang("death.attack.createnuclearized.scorching_heat", "%1$s was roasted on an Industrial Heater");
        REGISTRATE.addRawLang("death.attack.createnuclearized.scorching_heat.player",
                "%1$s was roasted on an Industrial Heater while fighting %2$s");

        REGISTRATE.addRawLang("effect.createnuclearized.radiation_sickness", "Radiation Sickness");
        REGISTRATE.addRawLang("effect.createnuclearized.blue_glow", "Bluebird Glow");

        REGISTRATE.addRawLang("item.minecraft.potion.effect.radiation_sickness", "Potion of Radiation Sickness");
        REGISTRATE.addRawLang("item.minecraft.splash_potion.effect.radiation_sickness", "Splash Potion of Radiation Sickness");
        REGISTRATE.addRawLang("item.minecraft.lingering_potion.effect.radiation_sickness", "Lingering Potion of Radiation Sickness");
        REGISTRATE.addRawLang("item.minecraft.tipped_arrow.effect.radiation_sickness", "Arrow of Radiation Sickness");
        REGISTRATE.addRawLang("item.minecraft.potion.effect.bluebird", "Potion of the Bluebird");
        REGISTRATE.addRawLang("item.minecraft.splash_potion.effect.bluebird", "Splash Potion of the Bluebird");
        REGISTRATE.addRawLang("item.minecraft.lingering_potion.effect.bluebird", "Lingering Potion of the Bluebird");
        REGISTRATE.addRawLang("item.minecraft.tipped_arrow.effect.bluebird", "Arrow of the Bluebird");
        REGISTRATE.addRawLang("death.attack.createnuclearized.radiation", "%1$s succumbed to radiation sickness");
        REGISTRATE.addRawLang("death.attack.createnuclearized.radiation.player",
                "%1$s succumbed to radiation sickness while fighting %2$s");

        REGISTRATE.addRawLang("createnuclearized.tooltip.turbine_rotor.steam", "Steam Use: %s mB/t");
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator", "Reactor Moderator");
        REGISTRATE.addRawLang("createnuclearized.tooltip.moderator.hint", "Sits between two channels, or a channel and a reflector");
        REGISTRATE.addRawLang("createnuclearized.tooltip.reflector", "Reactor Reflector");
        REGISTRATE.addRawLang("createnuclearized.tooltip.reflector.hint", "Line up as reflector, moderator, channel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.modifier", "Moderator Cooler");
        REGISTRATE.addRawLang("createnuclearized.tooltip.modifier.hint", "Place on a moderator's face");
        REGISTRATE.addRawLang("createnuclearized.tooltip.stat.burn", "Burn: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.stat.purity", "Purity: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.stat.heat", "Heat: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.stat.rate", "Rate: %s mB/t");
        REGISTRATE.addRawLang("createnuclearized.tooltip.shift_hint", "Hold Shift for Reactor Bonuses");
        REGISTRATE.addRawLang("createnuclearized.tooltip.ctrl_hint", "Hold Ctrl for burn & purity info");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.burn", "Burn: how much fuel reacts");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.burn.desc",
                "more heat & output, less unconsumed fuel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.purity", "Purity: quality of what reacts");
        REGISTRATE.addRawLang("createnuclearized.tooltip.help.purity.desc",
                "more useful material, less Off-Gas");
        REGISTRATE.addRawLang("createnuclearized.tooltip.precision_unmixer.flavor",
                "This machine does not obey the laws of physics at all");

        REGISTRATE.addRawLang("createnuclearized.tooltip.role.unconsumed", "(unconsumed)");
        REGISTRATE.addRawLang("createnuclearized.tooltip.role.bred", "(bred)");

        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_heat", "Has heat");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_heat.inverted", "Has no heat");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_unconsumed", "Has unconsumed fuel");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_unconsumed.inverted", "Has no unconsumed fuel");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_bred", "Has bred material");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_bred.inverted", "Has no bred material");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_off_gas", "Has off-gas");
        REGISTRATE.addRawLang("createnuclearized.item_attributes.has_off_gas.inverted", "Has no off-gas");

        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel", "Reactor Fuel");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.heat", "Max heat: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.burn", "Base burn: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.purity", "Base purity: %s%%");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.unconsumed", "Unconsumed: %s");
        REGISTRATE.addRawLang("createnuclearized.tooltip.fuel.bred", "Breeds: %s");
        REGISTRATE.addRawLang("element.createnuclearized.uranium", "Uranium");
        REGISTRATE.addRawLang("element.createnuclearized.plutonium", "Plutonium");
        REGISTRATE.addRawLang("element.createnuclearized.thorium", "Thorium");
        REGISTRATE.addRawLang("element.createnuclearized.americium", "Americium");

        REGISTRATE.addRawLang("itemGroup.createnuclearized.components", "Create: Nuclearized Components");

        REGISTRATE.addRawLang("createnuclearized.turbine_rotor.no_space", "Clear the highlighted blocks to place the rotor");

        REGISTRATE.addRawLang("createnuclearized.recipe.boiling", "Boiling");
        REGISTRATE.addRawLang("createnuclearized.recipe.heat_generation", "Heat Generation");
        REGISTRATE.addRawLang("createnuclearized.recipe.degassing", "Degassing");
        REGISTRATE.addRawLang("createnuclearized.recipe.unmixing", "Unmixing");
        REGISTRATE.addRawLang("createnuclearized.recipe.reconstitution", "Reconstitution");
        REGISTRATE.addRawLang("createnuclearized.recipe.gas_conversion", "Gas Conversion");
        REGISTRATE.addRawLang("createnuclearized.jei.gas_conversion_note", "Blow the gas through with an Encased Fan");
        REGISTRATE.addRawLang("createnuclearized.jei.gas_conversion_potion", "Copies the potion's effect onto the result");
        REGISTRATE.addRawLang("createnuclearized.jei.boil_cost", "Boil Cost: %s°/mB");
        REGISTRATE.addRawLang("createnuclearized.jei.heat_generated", "Generates %s heat per mB");
        REGISTRATE.addRawLang("createnuclearized.jei.rate", "Rate: %s mB/t");
        REGISTRATE.addRawLang("createnuclearized.jei.yield", "Yield: %s%%");
        REGISTRATE.addRawLang("createnuclearized.jei.hazard_damage", "Deals %s damage");
        REGISTRATE.addRawLang("createnuclearized.jei.hazard_ignite", "Sets on fire for %ss");
        REGISTRATE.addRawLang("createnuclearized.jei.hazard_potion", "Applies the potion's effects at %s%% of their duration");
        REGISTRATE.addRawLang("createnuclearized.jei.unmixing_pass", "One element per pass (example shown)");
        REGISTRATE.addRawLang("createnuclearized.jei.reconstitution_note", "Metal tops up the drained salt");

        REGISTRATE.addRawLang("createnuclearized.gui.controller.start", "Start");
        REGISTRATE.addRawLang("createnuclearized.gui.controller.stop", "Stop");
        REGISTRATE.addRawLang("createnuclearized.gui.controller.swap.locked", "Stop the reactor to swap drain sources");
        REGISTRATE.addRawLang("createnuclearized.gui.controller.swap.warning", "Attention: Pipes which are draining Output Valves might break!");
        REGISTRATE.addRawLang("createnuclearized.multiblock.state.stopped", "Stopped");
        REGISTRATE.addRawLang("createnuclearized.multiblock.state.idle", "Idle");
        REGISTRATE.addRawLang("createnuclearized.multiblock.state.running", "Running");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_controller.fissile_capacity", "Fissile Capacity: %s mB");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_controller.spent_capacity", "Spent Capacity: %s mB");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_controller.channels", "Channels: %s, Drain: %s");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_controller.drain.mixed", "Mixed");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_controller.drain.spent", "Spent");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_controller.drain.fuel", "Fuel");
        REGISTRATE.addRawLang("createnuclearized.gui.turbine_controller.steam", "Steam: %s / %s mB");
        REGISTRATE.addRawLang("createnuclearized.gui.turbine_controller.steam_use", "Steam Use: %s mB/t");
        REGISTRATE.addRawLang("createnuclearized.gui.turbine_controller.stages", "Stages: %s");
        REGISTRATE.addRawLang("createnuclearized.gui.turbine_controller.output", "Output: %s%%");
        REGISTRATE.addRawLang("createnuclearized.gui.turbine_controller.speed", "Speed: %s RPM");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.tab.info", "Reactor Info");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.tab.stats", "Output Stats");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.tab.interior", "Reactor Interior");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.modifiers", "Modifiers");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.no_modifiers", "No modifiers");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.affecting", "Affects selected valve");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.valve_hint",
                "Hold Shift for modifier info, or click to highlight");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.row", "%s: %s");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.fissile_buffer", "Fissile Buffer");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.spent_buffer", "Spent Buffer");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.channels", "Channels");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.max_flow", "Max Flow");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.runtime", "Runtime");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.heat", "Heat");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.off_gas", "Off-Gas");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.unconsumed", "Unconsumed");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.bred", "Bred");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.unknown_liquid", "Unknown. Input a liquid");
        REGISTRATE.addRawLang("createnuclearized.gui.reactor_monitor.unknown_value", "???");
        REGISTRATE.addRawLang("createnuclearized.gui.duration", "%sd %sh %sm %ss");
        REGISTRATE.addRawLang("createnuclearized.multiblock.too_large", "Structure is too large or malformed.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.shell_incomplete", "Shell is incomplete or has extra blocks attached.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.invalid", "Reactor invalid: %s");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.footprint",
                "Reactor width and depth must be between %s and %s (got %sx%s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.height", "Reactor height must be between %s and %s (got %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.wall", "Reactor wall is not solid casing at %s.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.rod_not_top", "Control rods must sit in the top face (at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.output_not_floor",
                "Spent-salt output valves must sit in the reactor floor (at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.controllers", "Reactor needs exactly one controller (found %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.no_input", "Reactor needs at least one fissile-salt input valve.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.no_output",
                "Reactor needs at least one spent-salt output valve in the floor.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.interior",
                "Reactor interior must be air, pipes, shafts, cogwheels, moderators or coolers (blocked at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.pipe_above_input", "No pipes allowed above the input valve (at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.no_pipes", "Run Create fluid pipes down through the reactor interior.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.no_pump", "Add a Create mechanical pump to drive the salt loop.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.input_unplumbed",
                "Connect a pipe to the inside of a fissile-salt input valve.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.channel_no_output",
                "The channel at %s needs a spent-salt output valve in the floor directly beneath it.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.output_no_channel",
                "The spent-salt output valve at %s needs a channel reaching down onto it.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.reactor.rod_blocked",
                "The control rod at %s needs air or a pipe directly beneath it.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.invalid", "Turbine invalid: %s");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.wall", "Turbine wall is not solid casing at %s.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.controllers", "Turbine needs exactly one controller (found %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.generators", "Turbine needs exactly one generator (found %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.no_steam_input", "Turbine needs at least one steam input valve.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.generator_position",
                "The generator must sit centered on a square end wall with an odd side (%s to %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.generator_facing",
                "The generator at %s must have its shaft facing into the turbine.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.cross_section",
                "Turbine cross section must be a square from %s to %s (got %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.depth", "Turbine depth must be between %s and %s (got %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.interior",
                "Turbine interior must be air, rotors, stators or the drive shaft (blocked at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.stator_without_rotor",
                "A stator needs a rotor in front of it (towards the generator).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.no_stages",
                "Build at least one stage by placing a turbine rotor on the axis.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.shaft_broken",
                "The center shaft must run unbroken along the turbine axis from the generator through the last rotor.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.shaft_across",
                "The center shaft must run along the turbine axis, not across it.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.axis_blocked",
                "Only the drive shaft or a rotor may sit on the turbine axis.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.around_shaft",
                "Only the drive shaft and stators may sit in a turbine slice (at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.rotor_axis", "A turbine rotor must face along the turbine axis.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.rotor_too_big",
                "The rotor is too big for this turbine's cross section.");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.rotor_obstructed", "The rotor's hitbox is obstructed (at %s).");
        REGISTRATE.addRawLang("createnuclearized.multiblock.turbine.rotor_sweep", "Nothing may sit in the rotor's sweep (at %s).");

        REGISTRATE.addRawLang("advancements.createnuclearized.main.title", "Create: Nuclearized");
        REGISTRATE.addRawLang("advancements.createnuclearized.main.description",
                "Dig into reactors, turbines and radioactive plumbing.");
        REGISTRATE.addRawLang("advancements.createnuclearized.dont_eat_that.title", "Galen, don't eat that!");
        REGISTRATE.addRawLang("advancements.createnuclearized.dont_eat_that.description",
                "Pick up a piece of raw thorium. Not for consumption.");
        REGISTRATE.addRawLang("advancements.createnuclearized.i_flibe_you.title", "I FLiBe You");
        REGISTRATE.addRawLang("advancements.createnuclearized.i_flibe_you.description",
                "Get a bucket of molten salt. The carrier of all good chemistry.");
        REGISTRATE.addRawLang("advancements.createnuclearized.not_a_centrifuge.title", "It's Not a Centrifuge!");
        REGISTRATE.addRawLang("advancements.createnuclearized.not_a_centrifuge.description",
                "Craft a Precision Unmixer and start taking salts apart.");
        REGISTRATE.addRawLang("advancements.createnuclearized.lots_of_heat.title", "Lots of Heat");
        REGISTRATE.addRawLang("advancements.createnuclearized.lots_of_heat.description",
                "Start a reactor and watch the salts flow.");
        REGISTRATE.addRawLang("advancements.createnuclearized.that_tingles.title", "That Tingles!");
        REGISTRATE.addRawLang("advancements.createnuclearized.that_tingles.description",
                "Stand in the vapor above a working Evaporation Basin.");
        REGISTRATE.addRawLang("advancements.createnuclearized.master_of_pneumatics.title", "Master of Pneumatics");
        REGISTRATE.addRawLang("advancements.createnuclearized.master_of_pneumatics.description",
                "Spin up a valid turbine with pressurized steam.");
        REGISTRATE.addRawLang("advancements.createnuclearized.spacecraft_magic.title", "Spacecraft Magic");
        REGISTRATE.addRawLang("advancements.createnuclearized.spacecraft_magic.description",
                "Place a Radioisotope Thermoelectric Generator (RTG) and let it spin forever.");

        REGISTRATE.addRawLang("advancements.createnuclearized.liquids.title", "Nuclearized Liquids");
        REGISTRATE.addRawLang("advancements.createnuclearized.liquids.description",
                "Convert fuel salts in the reactor into their spent equivalent.");
        REGISTRATE.addRawLang("advancements.createnuclearized.salty_uranium.title", "Salty Uranium");
        REGISTRATE.addRawLang("advancements.createnuclearized.salty_uranium.description",
                "Convert uranium salt to spent uranium salt in your reactor.");
        REGISTRATE.addRawLang("advancements.createnuclearized.salty_plutonium.title", "Salty Plutonium");
        REGISTRATE.addRawLang("advancements.createnuclearized.salty_plutonium.description",
                "Convert plutonium salt to spent plutonium salt in your reactor.");
        REGISTRATE.addRawLang("advancements.createnuclearized.salty_thorium_uranium.title", "Salty Thorium-Uranium");
        REGISTRATE.addRawLang("advancements.createnuclearized.salty_thorium_uranium.description",
                "Convert thorium-uranium salt to spent thorium-uranium salt in your reactor.");
    }
}
