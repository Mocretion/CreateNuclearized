package com.mocretion.createnuclearized.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class CNTags {

    public static final TagKey<Block> THORIUM_ORES = block("ores/thorium");
    public static final TagKey<Block> URANIUM_ORES = block("ores/uranium");

    public static final TagKey<Item> THORIUM_ORE_ITEMS = item("ores/thorium");
    public static final TagKey<Item> URANIUM_ORE_ITEMS = item("ores/uranium");
    public static final TagKey<Item> RAW_THORIUM = item("raw_materials/thorium");
    public static final TagKey<Item> RAW_URANIUM = item("raw_materials/uranium");

    public static final TagKey<Item> CRUSHED_RAW_MATERIALS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_materials"));

    private CNTags() {}

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
