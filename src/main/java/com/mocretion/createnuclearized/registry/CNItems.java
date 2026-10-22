package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;

public final class CNItems {

    public static final ItemEntry<Item> RAW_THORIUM = REGISTRATE
            .item("raw_thorium", Item::new)
            .tag(Tags.Items.RAW_MATERIALS, CNTags.RAW_THORIUM)
            .register();

    public static final ItemEntry<Item> CRUSHED_RAW_THORIUM = REGISTRATE
            .item("crushed_raw_thorium", Item::new)
            .tag(CNTags.CRUSHED_RAW_MATERIALS)
            .register();

    public static final ItemEntry<Item> RAW_URANIUM = REGISTRATE
            .item("raw_uranium", Item::new)
            .tag(Tags.Items.RAW_MATERIALS, CNTags.RAW_URANIUM)
            .register();

    public static final ItemEntry<Item> XENON_CRYSTALS = REGISTRATE
            .item("xenon_crystals", Item::new)
            .tag(Tags.Items.GEMS)
            .register();

    public static final ItemEntry<Item> BERYLLIUM = REGISTRATE
            .item("beryllium", Item::new)
            .register();

    public static final ItemEntry<Item> GRAPHITE = REGISTRATE
            .item("graphite", Item::new)
            .register();

    public static final ItemEntry<Item> STEEL_INGOT = REGISTRATE
            .item("steel_ingot", Item::new)
            .tag(Tags.Items.INGOTS, CNTags.STEEL_INGOTS)
            .register();

    public static final ItemEntry<Item> STEEL_SHEET = REGISTRATE
            .item("steel_sheet", Item::new)
            .tag(CNTags.PLATES, CNTags.STEEL_PLATES)
            .register();

    public static final ItemEntry<Item> REACTOR_PLATING = REGISTRATE
            .item("reactor_plating", Item::new)
            .register();

    public static final ItemEntry<Item> CONTROL_UNIT = REGISTRATE
            .item("control_unit", Item::new)
            .register();

    public static final ItemEntry<Item> TURBINE_BLADE = REGISTRATE
            .item("turbine_blade", Item::new)
            .model((c, p) -> p.generated(c,
                    ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "item/steel_sheet")))
            .register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_REACTOR_PLATING = incomplete("incomplete_reactor_plating");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_REACTOR_GLASS = incomplete("incomplete_reactor_glass");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_CONTROL_ROD = incomplete("incomplete_control_rod");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_CONTROL_UNIT = incomplete("incomplete_control_unit");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TURBINE_CASING = incomplete("incomplete_turbine_casing");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TURBINE_BLADE = incomplete("incomplete_turbine_blade");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TURBINE_ROTOR = incomplete("incomplete_turbine_rotor");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_LARGE_TURBINE_ROTOR = incomplete("incomplete_large_turbine_rotor");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_INDUSTRIAL_HEATER = incomplete("incomplete_industrial_heater");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_PRECISION_UNMIXER = incomplete("incomplete_precision_unmixer");

    private CNItems() {}

    private static ItemEntry<SequencedAssemblyItem> incomplete(String name) {
        return REGISTRATE
                .item(name, SequencedAssemblyItem::new)
                .model((c, p) -> {})
                .removeTab(CreateNuclearized.MAIN_TAB_KEY)
                .register();
    }

    public static void register() {}
}
