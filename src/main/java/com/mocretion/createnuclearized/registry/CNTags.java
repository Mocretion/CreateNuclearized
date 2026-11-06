package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;

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
    public static final TagKey<Item> STEEL_INGOTS = item("ingots/steel");
    public static final TagKey<Item> STEEL_PLATES = item("plates/steel");
    public static final TagKey<Item> PLATES = item("plates");
    public static final TagKey<Item> BUCKETS = item("buckets");

    public static final TagKey<Block> STEEL_BLOCKS = block("storage_blocks/steel");
    public static final TagKey<Item> STEEL_BLOCK_ITEMS = item("storage_blocks/steel");

    public static final TagKey<Block> RAW_URANIUM_BLOCKS = block("storage_blocks/raw_uranium");
    public static final TagKey<Item> RAW_URANIUM_BLOCK_ITEMS = item("storage_blocks/raw_uranium");

    public static final TagKey<Block> GLASS_BLOCKS = block("glass_blocks");
    public static final TagKey<Block> GLASS_BLOCKS_COLORLESS = block("glass_blocks/colorless");
    public static final TagKey<Block> IMPERMEABLE = TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("impermeable"));

    public static final TagKey<Item> GLASS_BLOCK_ITEMS = item("glass_blocks");
    public static final TagKey<Item> GLASS_BLOCK_COLORLESS_ITEMS = item("glass_blocks/colorless");

    public static final TagKey<Block> REACTOR_PARTS_SHELL = modBlock("reactor_parts/shell");
    public static final TagKey<Block> REACTOR_PARTS_INTERIOR = modBlock("reactor_parts/interior");
    public static final TagKey<Block> TURBINE_PARTS_SHELL = modBlock("turbine_parts/shell");
    public static final TagKey<Block> TURBINE_PARTS_INTERIOR = modBlock("turbine_parts/interior");
    public static final TagKey<Block> REACTOR_MODIFIER_MODERATOR = modBlock("reactor_modifier/moderator");
    public static final TagKey<Block> REACTOR_MODIFIER_COOLER = modBlock("reactor_modifier/cooler");
    public static final TagKey<Block> REACTOR_MODIFIER_REFLECTOR = modBlock("reactor_modifier/reflector");

    public static final TagKey<Item> CRUSHED_RAW_MATERIALS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_materials"));

    private CNTags() {}

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Block> modBlock(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
