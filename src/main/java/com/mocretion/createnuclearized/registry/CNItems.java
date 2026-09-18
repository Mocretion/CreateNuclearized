package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.content.boiler.HazmatHelmetItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

public final class CNItems {

    // Uranium fuel
    // The salt design dissolves fuel into the salt instead of feeding an item, so this is just a placeholder ingredient for a future
    // molten salt recipe. Glowstone dust texture
    public static final ItemEntry<Item> URANIUM_FUEL = REGISTRATE
            .item("uranium_fuel", Item::new)
            .model((c, p) -> p.generated(c, ResourceLocation.withDefaultNamespace("item/glowstone_dust")))
            .register();

    // Wearable head armor, and right clicked onto a blaze burner to cap it into a contained blaze burner Golden helmet texture for now
    // Armor durability comes from the Properties, not the material, so set it the same way the golden helmet does (else it stacks to 64)
    public static final ItemEntry<HazmatHelmetItem> HAZMAT_HELMET = REGISTRATE
            .item("hazmat_helmet", HazmatHelmetItem::new)
            .properties(p -> p.durability(ArmorItem.Type.HELMET.getDurability(7)))
            .model((c, p) -> p.generated(c, ResourceLocation.withDefaultNamespace("item/golden_helmet")))
            .register();

    private CNItems() {}

    /** No op that forces this class to load so the fields above initialize */
    public static void register() {}
}
