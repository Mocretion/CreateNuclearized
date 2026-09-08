package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class CNItems {

    // Uranium fuel
    // The salt design dissolves fuel into the salt instead of feeding an item, so this is just a placeholder ingredient for a future
    // molten salt recipe. Glowstone dust texture
    public static final ItemEntry<Item> URANIUM_FUEL = REGISTRATE
            .item("uranium_fuel", Item::new)
            .model((c, p) -> p.generated(c, ResourceLocation.withDefaultNamespace("item/glowstone_dust")))
            .register();

    private CNItems() {}

    /** No op that forces this class to load so the fields above initialize */
    public static void register() {}
}
