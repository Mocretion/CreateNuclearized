package com.mocretion.createnuclearized;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mocretion.createnuclearized.content.filter.SaltStatsAttribute;
import com.mocretion.createnuclearized.content.fluid.SpentSaltBucketWrapper;
import com.mocretion.createnuclearized.content.metal.Metals;
import com.mocretion.createnuclearized.content.multiblock.MultiblockErrorPayload;
import com.mocretion.createnuclearized.content.radiation.CNEffects;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFeatures;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNItems;
import com.mocretion.createnuclearized.registry.CNMenuTypes;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
                        output.accept(CNBlocks.GRAPHITE_BLOCK.get());
                        output.accept(Blocks.SEA_LANTERN);
                        output.accept(Blocks.GLOWSTONE);
                        output.accept(Blocks.SCULK);
                        output.accept(Blocks.SPONGE);
                        output.accept(Blocks.AMETHYST_BLOCK);
                        output.accept(Blocks.HONEY_BLOCK);
                        output.accept(CNBlocks.XENON_BLOCK.get());
                        output.accept(Blocks.DIAMOND_BLOCK);
                        output.accept(Items.WATER_BUCKET);
                        output.accept(CNBlocks.BERYLLIUM_BLOCK.get());
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

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::buildCreativeTabContents);
        modEventBus.addListener(MultiblockErrorPayload::register);
        modEventBus.addListener(SaltStatsAttribute::register);

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
        acceptWithBaseStats(event, CNFluids.spentUraniumSalt());
        acceptWithBaseStats(event, CNFluids.spentPlutoniumSalt());
        acceptWithBaseStats(event, CNFluids.spentThoriumUraniumSalt());
        Metals.addCreativeItems(event);
    }

    private static void acceptWithBaseStats(BuildCreativeModeTabContentsEvent event, Fluid spent) {
        ItemStack bucket = new ItemStack(spent.getBucket());
        ReactorFuel fuel = ReactorFuel.forOutput(spent);
        if (fuel != null)
            bucket.set(CNDataComponents.SALT_STATS.get(), fuel.baseComposition());
        event.accept(bucket);
    }
}
