package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.content.boiler.HazmatHelmetItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
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

    public static final ItemEntry<HazmatHelmetItem> HAZMAT_HELMET = REGISTRATE
            .item("hazmat_helmet", HazmatHelmetItem::new)
            .properties(p -> p.durability(ArmorItem.Type.HELMET.getDurability(7)))
            .model((c, p) -> p.generated(c, ResourceLocation.withDefaultNamespace("item/golden_helmet")))
            .register();

    private CNItems() {}

    public static void register() {}
}
