package com.mocretion.createnuclearized;

import org.slf4j.Logger;

import com.mocretion.createnuclearized.content.fluid.DepletedSaltBucketWrapper;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNItems;
import com.mocretion.createnuclearized.registry.CNMenuTypes;
import com.mocretion.createnuclearized.registry.CNRegistrate;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CreateNuclearized.MODID)
public class CreateNuclearized {
    public static final String MODID = "createnuclearized";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The tab is a plain vanilla registration, but its contents come from Registrate (pointed at this tab in the constructor), so every
    // registered item lands here on its own
    // Dont list them in displayItems too, or they get added twice and the creative inventory crashes
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final ResourceKey<CreativeModeTab> MAIN_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MODID, "main"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createnuclearized"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> CNBlocks.REACTOR_CONTROLLER.asStack())
                    .build());

    public static final ResourceKey<CreativeModeTab> COMPONENTS_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MODID, "components"));

    // Second tab for the reactors stat blocks
    // Filled explicitly here rather than through Registrates default tab because most of the modifiers are vanilla blocks
    // Listing an item in more than one tab is fine (the search tab dedupes with a Set); only adding the same item to the same tab twice
    // crashes
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMPONENTS_TAB = CREATIVE_MODE_TABS.register(
            "components", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createnuclearized.components"))
                    .withTabsAfter(MAIN_TAB_KEY)
                    .icon(() -> CNBlocks.GRAPHITE_BLOCK.asStack())
                    .displayItems((params, output) -> {
                        // Moderators first
                        output.accept(CNBlocks.GRAPHITE_BLOCK.get());
                        output.accept(CNBlocks.BERYLLIUM_BLOCK.get());
                        // Then coolers, grouped by which stat they touch
                        output.accept(Blocks.SEA_LANTERN);   // burn
                        output.accept(Blocks.GLOWSTONE);
                        output.accept(Blocks.SCULK);
                        output.accept(Blocks.SPONGE);         // purity
                        output.accept(Blocks.AMETHYST_BLOCK);
                        output.accept(Blocks.HONEY_BLOCK);
                        output.accept(CNBlocks.XENON_BLOCK.get()); // rate
                        output.accept(Blocks.DIAMOND_BLOCK);
                        output.accept(Items.WATER_BUCKET);
                    })
                    .build());

    public CreateNuclearized(IEventBus modEventBus, ModContainer modContainer) {
        // Set the default creative tab before any content class loads, so registered items land in MAIN_TAB and not the search tab (where
        // they'd double add and crash the inventory)
        // The register() calls just force the registry holders to class load and fire their events
        CNRegistrate.REGISTRATE.registerEventListeners(modEventBus);
        CNRegistrate.REGISTRATE.defaultCreativeTab(MAIN_TAB_KEY);
        CNBlocks.register();
        CNBlockEntities.register();
        CNItems.register();
        CNFluids.register();
        CNDataComponents.register();
        CNRegistrate.registerRawLang();

        CREATIVE_MODE_TABS.register(modEventBus);
        CNMenuTypes.MENUS.register(modEventBus);
        CNDataComponents.TYPES.register(modEventBus);

        // Fluid handlers on the valves, so Create pipes/pumps can move salt through them
        modEventBus.addListener(this::registerCapabilities);
        // Give the creative depleted salt buckets their fuels base stats
        modEventBus.addListener(this::buildCreativeTabContents);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("Create: Nuclearized loaded");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.REACTOR_FLUID_VALVE.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.STEAM_INPUT_VALVE.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.CONTAINED_BLAZE_BURNER.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.EXHAUST_HOOD.get(),
                (be, side) -> be.getFluidHandler());
        // Component aware bucket handler on the depleted salt buckets, so filling/emptying carries the SaltStats
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new DepletedSaltBucketWrapper(stack),
                CNFluids.depletedUraniumSalt().getBucket(),
                CNFluids.depletedPlutoniumSalt().getBucket(),
                CNFluids.depletedThoriumUraniumSalt().getBucket());
    }

    // The depleted salt buckets are kept out of the default tab (in CNFluids) and re added here stamped with their fuels base composition
    private void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(MAIN_TAB_KEY)) return;
        acceptWithBaseStats(event, CNFluids.depletedUraniumSalt());
        acceptWithBaseStats(event, CNFluids.depletedPlutoniumSalt());
        acceptWithBaseStats(event, CNFluids.depletedThoriumUraniumSalt());
    }

    private static void acceptWithBaseStats(BuildCreativeModeTabContentsEvent event, Fluid depleted) {
        ItemStack bucket = new ItemStack(depleted.getBucket());
        ReactorFuel fuel = ReactorFuel.forOutput(depleted);
        if (fuel != null)
            bucket.set(CNDataComponents.SALT_STATS.get(), fuel.baseComposition());
        event.accept(bucket);
    }
}
