package com.mocretion.createnuclearized;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mocretion.createnuclearized.content.advancement.CNCriteriaTriggers;
import com.mocretion.createnuclearized.content.command.CNCommands;
import com.mocretion.createnuclearized.content.compactingfrommelt.CompactingFromMeltLoader;
import com.mocretion.createnuclearized.content.filter.SaltStatsAttribute;
import com.mocretion.createnuclearized.content.fluid.SpentSaltBucketWrapper;
import com.mocretion.createnuclearized.content.metal.Metals;
import com.mocretion.createnuclearized.content.multiblock.MultiblockErrorPayload;
import com.mocretion.createnuclearized.content.potion.CNPotions;
import com.mocretion.createnuclearized.content.radiation.CNEffects;
import com.mocretion.createnuclearized.content.reactor.ReactorComponents;
import com.mocretion.createnuclearized.content.reactor.ReactorComponentsTagPack;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFanProcessingTypes;
import com.mocretion.createnuclearized.registry.CNFeatures;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNItems;
import com.mocretion.createnuclearized.registry.CNMenuTypes;
import com.mocretion.createnuclearized.registry.CNParticleTypes;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.mocretion.createnuclearized.registry.CNRegistrate;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CreateNuclearized.MODID)
public class CreateNuclearized {
    public static final String MODID = "createnuclearized";
    public static final Logger LOGGER = LogUtils.getLogger();

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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMPONENTS_TAB = CREATIVE_MODE_TABS.register(
            "components", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createnuclearized.components"))
                    .withTabsAfter(MAIN_TAB_KEY)
                    .icon(() -> CNBlocks.GRAPHITE_BLOCK.asStack())
                    .displayItems((params, output) -> {
                        for (Block block : ReactorComponents.moderatorBlocks())
                            output.accept(block);
                        for (Block block : ReactorComponents.coolerBlocks())
                            output.accept(block);
                        for (Block block : ReactorComponents.reflectorBlocks())
                            output.accept(block);
                    })
                    .build());

    public CreateNuclearized(IEventBus modEventBus) {
        CNRegistrate.REGISTRATE.registerEventListeners(modEventBus);
        CNBlocks.register();
        CNBlockEntities.register();
        CNItems.register();
        CNFluids.register();
        Metals.init(modEventBus);
        CNDataComponents.register();
        CNRegistrate.registerRawLang();

        CREATIVE_MODE_TABS.register(modEventBus);
        CNMenuTypes.MENUS.register(modEventBus);
        CNRecipeTypes.TYPES.register(modEventBus);
        CNRecipeTypes.SERIALIZERS.register(modEventBus);
        CNDataComponents.TYPES.register(modEventBus);
        CNFeatures.FEATURES.register(modEventBus);
        CNEffects.EFFECTS.register(modEventBus);
        CNPotions.POTIONS.register(modEventBus);
        CNParticleTypes.TYPES.register(modEventBus);
        CNFanProcessingTypes.TYPES.register(modEventBus);
        CNCriteriaTriggers.TRIGGERS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::buildCreativeTabContents);
        modEventBus.addListener(MultiblockErrorPayload::register);
        modEventBus.addListener(SaltStatsAttribute::register);
        modEventBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ReactorComponents::initDefaults));
        modEventBus.addListener(ReactorComponentsTagPack::addPackFinders);

        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new CompactingFromMeltLoader(e.getServerResources().getRecipeManager()));
            e.addListener(new ReactorComponents());
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> CNCommands.register(e.getDispatcher()));

        LOGGER.info("Create: Nuclearized loaded");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.REACTOR_FLUID_VALVE.get(),
                (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.TURBINE_STEAM_INPUT_VALVE.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.INDUSTRIAL_HEATER.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.EVAPORATION_BASIN.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.EXHAUST_HOOD.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CNBlockEntities.METERING_VALVE.get(),
                (be, side) -> be.getFluidHandler(side));
        List<Item> spentBuckets = new ArrayList<>(List.of(
                CNFluids.spentUraniumSalt().getBucket(),
                CNFluids.spentPlutoniumSalt().getBucket(),
                CNFluids.spentThoriumUraniumSalt().getBucket()));
        spentBuckets.addAll(Metals.spentBuckets());
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new SpentSaltBucketWrapper(stack),
                spentBuckets.toArray(Item[]::new));
    }

    private void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(MAIN_TAB_KEY)) return;
        List<ItemStack> ordered = new ArrayList<>();

        add(ordered, CNBlocks.THORIUM_ORE.get());
        add(ordered, CNBlocks.DEEPSLATE_THORIUM_ORE.get());
        add(ordered, CNItems.RAW_THORIUM.get());
        add(ordered, CNItems.CRUSHED_RAW_THORIUM.get());
        add(ordered, CNBlocks.SCULK_URANIUM_ORE.get());
        add(ordered, CNItems.RAW_URANIUM.get());
        add(ordered, CNBlocks.RAW_URANIUM_BLOCK.get());
        add(ordered, CNBlocks.MONAZITE_SAND.get());

        add(ordered, CNItems.GRAPHITE.get());
        add(ordered, CNBlocks.GRAPHITE_BLOCK.get());
        add(ordered, CNItems.BERYLLIUM.get());
        add(ordered, CNBlocks.BERYLLIUM_BLOCK.get());
        add(ordered, CNItems.XENON_CRYSTALS.get());
        add(ordered, CNBlocks.XENON_BLOCK.get());
        add(ordered, CNItems.STEEL_INGOT.get());
        add(ordered, CNItems.STEEL_SHEET.get());
        add(ordered, CNBlocks.STEEL_BLOCK.get());

        add(ordered, CNBlocks.REACTOR_CONTROLLER.get());
        add(ordered, CNBlocks.REACTOR_MONITOR.get());
        add(ordered, CNBlocks.REACTOR_CASING.get());
        add(ordered, CNBlocks.REACTOR_GLASS.get());
        add(ordered, CNBlocks.REACTOR_SALT_INPUT_VALVE.get());
        add(ordered, CNBlocks.REACTOR_SALT_OUTPUT_VALVE.get());
        add(ordered, CNBlocks.REACTOR_ENCASED_CHAIN_DRIVE.get());
        add(ordered, CNBlocks.CONTROL_ROD.get());

        add(ordered, CNBlocks.TURBINE_CONTROLLER.get());
        add(ordered, CNBlocks.TURBINE_GENERATOR.get());
        add(ordered, CNBlocks.TURBINE_CASING.get());
        add(ordered, CNBlocks.TURBINE_GLASS.get());
        add(ordered, CNBlocks.TURBINE_STEAM_INPUT_VALVE.get());
        add(ordered, CNBlocks.TURBINE_ROTOR.get());
        add(ordered, CNBlocks.LARGE_TURBINE_ROTOR.get());
        add(ordered, CNBlocks.TURBINE_STATOR.get());

        add(ordered, CNBlocks.PRECISION_UNMIXER.get());
        add(ordered, CNBlocks.INDUSTRIAL_HEATER.get());
        add(ordered, CNBlocks.EVAPORATION_BASIN.get());
        add(ordered, CNBlocks.EXHAUST_HOOD.get());
        add(ordered, CNBlocks.METERING_VALVE.get());
        add(ordered, CNBlocks.PLUTONIUM_RTG.get());
        add(ordered, CNBlocks.AMERICIUM_RTG.get());

        add(ordered, CNItems.REACTOR_PLATING.get());
        add(ordered, CNItems.CONTROL_UNIT.get());
        add(ordered, CNItems.TURBINE_BLADE.get());

        add(ordered, CNFluids.PRESSURIZED_STEAM.get().getBucket());
        add(ordered, CNFluids.OFF_GAS.get().getBucket());
        add(ordered, CNFluids.LIQUID_THORIUM.get().getBucket());
        add(ordered, CNFluids.LIQUID_THORIUM_URANIUM.get().getBucket());
        add(ordered, CNFluids.THORIUM_URANIUM_SALT.get().getBucket());
        ordered.add(spentWithBaseStats(CNFluids.spentThoriumUraniumSalt()));
        add(ordered, CNFluids.LIQUID_URANIUM.get().getBucket());
        add(ordered, CNFluids.ENRICHED_LIQUID_URANIUM.get().getBucket());
        add(ordered, CNFluids.URANIUM_SALT.get().getBucket());
        ordered.add(spentWithBaseStats(CNFluids.spentUraniumSalt()));
        add(ordered, CNFluids.LIQUID_PLUTONIUM.get().getBucket());
        add(ordered, CNFluids.PLUTONIUM_SALT.get().getBucket());
        ordered.add(spentWithBaseStats(CNFluids.spentPlutoniumSalt()));
        add(ordered, CNFluids.LIQUID_AMERICIUM.get().getBucket());
        add(ordered, CNFluids.MOLTEN_SALT.get().getBucket());

        for (ItemStack stack : ordered)
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        for (ItemStack stack : ordered)
            event.accept(stack);

        Metals.addCreativeItems(event);
    }

    private static void add(List<ItemStack> ordered, ItemLike item) {
        ordered.add(new ItemStack(item));
    }

    private static ItemStack spentWithBaseStats(Fluid spent) {
        ItemStack bucket = new ItemStack(spent.getBucket());
        ReactorFuel fuel = ReactorFuel.forOutput(spent);
        if (fuel != null)
            bucket.set(CNDataComponents.SALT_STATS.get(), fuel.baseComposition());
        return bucket;
    }
}
