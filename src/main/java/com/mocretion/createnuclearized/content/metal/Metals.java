package com.mocretion.createnuclearized.content.metal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.fluid.SpentSaltBucketItem;
import com.mocretion.createnuclearized.content.fluid.SpentSaltLiquidBlock;
import com.mocretion.createnuclearized.content.fluid.TexturedFluidType;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltElement;
import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Metals {

    private static final ResourceLocation LIQUID_STILL = texture("block/liquid_still");
    private static final ResourceLocation LIQUID_FLOW = texture("block/liquid_flow");
    private static final ResourceLocation LIQUID_HOT_STILL = texture("block/liquid_hot_still");
    private static final ResourceLocation LIQUID_HOT_FLOW = texture("block/liquid_hot_flow");

    private static final int METAL_TICK_RATE = 25;
    private static final int METAL_LEVEL_DECREASE = 2;
    private static final int METAL_SLOPE_DISTANCE = 2;

    private static final List<LoadedMetal> LOADED = new ArrayList<>();
    private static final Map<String, Registers> REGISTERS = new HashMap<>();
    private static List<Path> packSources = List.of();

    private Metals() {}

    public record LoadedMetal(MetalDefinition definition, @Nullable FluidSet liquid, @Nullable FluidSet enriched,
            @Nullable FluidSet salt, @Nullable FluidSet spentSalt, SaltElement element, @Nullable ReactorFuel fuel) {

        List<FluidSet> sets() {
            List<FluidSet> sets = new ArrayList<>();
            for (FluidSet set : new FluidSet[] { liquid, enriched, salt, spentSalt }) {
                if (set != null) sets.add(set);
            }
            return sets;
        }
    }

    public record FluidSet(Supplier<Fluid> source, Supplier<Item> bucket, Supplier<Block> block) {}

    private record SaltFluids(FluidSet salt, FluidSet spentSalt, ReactorFuel fuel) {}

    private record Registers(DeferredRegister<Fluid> fluids, DeferredRegister<FluidType> types, DeferredRegister<Block> blocks,
            DeferredRegister<Item> items) {}

    private record FluidSpec(String name, int tint, int radiation, boolean metal, boolean spent, int temperature) {}

    public static void init(IEventBus modBus) {
        packSources = MetalScanner.packSources();
        List<MetalDefinition> definitions = MetalScanner.scan(packSources);

        List<MetalDefinition> created = new ArrayList<>();
        List<MetalDefinition> overrides = new ArrayList<>();
        for (MetalDefinition definition : definitions) {
            if (MetalOverrides.isBuiltin(definition.id())) {
                MetalOverrides.add(definition);
                overrides.add(definition);
            } else {
                created.add(definition);
            }
        }

        Map<MetalDefinition, SaltElement> elements = new HashMap<>();
        for (MetalDefinition definition : created) {
            SaltElement element = SaltElement.register(definition.elementId(), () -> liquidOf(definition));
            elements.put(definition, element);
        }
        for (MetalDefinition definition : created)
            register(modBus, definition, elements.get(definition));
        for (MetalDefinition definition : overrides)
            registerOverrideExtras(modBus, definition);
        MetalOverrides.applyFuelStats();

        modBus.addListener(Metals::addPackFinders);
        modBus.addListener(Metals::addValidBlocks);
        CreateNuclearized.LOGGER.info("Loaded {} data driven metal(s) and {} override(s)", created.size(), overrides.size());
    }

    public static List<Item> allBuckets() {
        List<Item> buckets = new ArrayList<>();
        for (LoadedMetal metal : LOADED) {
            for (FluidSet set : metal.sets())
                buckets.add(set.bucket().get());
        }
        return buckets;
    }

    public static List<Item> spentBuckets() {
        return LOADED.stream().filter(metal -> metal.spentSalt() != null).map(metal -> metal.spentSalt().bucket().get()).toList();
    }

    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        for (LoadedMetal metal : LOADED) {
            for (FluidSet set : metal.sets()) {
                if (set == metal.spentSalt()) continue;
                event.accept(set.bucket().get());
            }
            if (metal.spentSalt() == null || metal.fuel() == null) continue;
            ItemStack spent = new ItemStack(metal.spentSalt().bucket().get());
            spent.set(CNDataComponents.SALT_STATS.get(), metal.fuel().baseComposition());
            event.accept(spent);
        }
    }

    private static Fluid liquidOf(MetalDefinition definition) {
        for (LoadedMetal metal : LOADED) {
            if (metal.definition() == definition && metal.liquid() != null) return metal.liquid().source().get();
        }
        throw new IllegalStateException("Metal not registered: " + definition.id());
    }

    private static void register(IEventBus modBus, MetalDefinition definition, SaltElement element) {
        String namespace = definition.id().getNamespace();
        Registers registers = registersFor(modBus, namespace);

        FluidSet liquid = registerFluid(registers, namespace,
                new FluidSpec(definition.liquidId().getPath(), definition.color(), definition.radiation(), true, false, 0));
        FluidSet enriched = definition.enriched().orElse(false) ? registerEnriched(registers, namespace, definition) : null;
        SaltFluids salts = definition.hasSalt() ? registerSalts(registers, namespace, definition, element, enriched) : null;

        LOADED.add(new LoadedMetal(definition, liquid, enriched, salts == null ? null : salts.salt(),
                salts == null ? null : salts.spentSalt(), element, salts == null ? null : salts.fuel()));
    }

    private static void registerOverrideExtras(IEventBus modBus, MetalDefinition definition) {
        String namespace = definition.id().getNamespace();
        String slug = definition.slug();
        boolean builtinHasSalt = slug.equals("uranium") || slug.equals("plutonium") || slug.equals("thorium_uranium");
        SaltElement element = SaltElement.byId(definition.elementId());
        Registers registers = registersFor(modBus, namespace);

        FluidSet enriched = null;
        if (definition.enriched().orElse(false) && !slug.equals("uranium"))
            enriched = registerEnriched(registers, namespace, definition);

        SaltFluids salts = null;
        if (!builtinHasSalt && element != null && definition.salt().enabled().orElse(false))
            salts = registerSalts(registers, namespace, definition, element, enriched);

        if (enriched == null && salts == null) return;
        SaltElement owner = element != null ? element : SaltElement.URANIUM;
        LOADED.add(new LoadedMetal(definition, null, enriched, salts == null ? null : salts.salt(),
                salts == null ? null : salts.spentSalt(), owner, salts == null ? null : salts.fuel()));
    }

    private static FluidSet registerEnriched(Registers registers, String namespace, MetalDefinition definition) {
        return registerFluid(registers, namespace,
                new FluidSpec(definition.enrichedLiquidId().getPath(), definition.enrichedColor(), definition.radiation(), true, false, 0));
    }

    private static SaltFluids registerSalts(Registers registers, String namespace, MetalDefinition definition, SaltElement element,
            @Nullable FluidSet enriched) {
        MetalDefinition.Salt salt = definition.salt();

        FluidSet saltFluid = registerFluid(registers, namespace,
                new FluidSpec(definition.saltId().getPath(), definition.saltColor(), definition.radiation(), false, false, 0));
        FluidSet spent = registerFluid(registers, namespace,
                new FluidSpec(definition.spentSaltId().getPath(), definition.spentSaltColor(), definition.radiation(), false, true, 0));

        SaltElement unconsumed = resolve(salt.unconsumed(), element);
        Supplier<Fluid> unconsumedLiquid = enriched != null && unconsumed == element ? enriched.source() : null;
        ReactorFuel fuel = ReactorFuel.register(salt.maxHeatOrDefault(), salt.burnOrDefault(), salt.purityOrDefault(),
                unconsumed, resolveBred(salt.bred()), saltFluid.source(), spent.source(), unconsumedLiquid);
        return new SaltFluids(saltFluid, spent, fuel);
    }

    private static Registers registersFor(IEventBus modBus, String namespace) {
        return REGISTERS.computeIfAbsent(namespace, ns -> createRegisters(modBus, ns));
    }

    @Nullable
    private static SaltElement resolve(Optional<String> id, SaltElement fallback) {
        if (id.filter(MetalDefinition.NO_ELEMENT::equals).isPresent()) return null;
        SaltElement resolved = id.map(Metals::lookup).orElse(null);
        return resolved != null ? resolved : fallback;
    }

    @Nullable
    private static SaltElement resolveBred(Optional<String> id) {
        return id.filter(text -> !MetalDefinition.NO_ELEMENT.equals(text)).map(Metals::lookup).orElse(null);
    }

    @Nullable
    static SaltElement lookup(String text) {
        ResourceLocation id = text.contains(":") ? ResourceLocation.tryParse(text) : ResourceLocation.tryBuild(CreateNuclearized.MODID, text);
        SaltElement element = id == null ? null : SaltElement.byId(id);
        if (element == null)
            CreateNuclearized.LOGGER.error("Unknown salt element {} referenced by a metal definition", text);
        return element;
    }

    private static Registers createRegisters(IEventBus modBus, String namespace) {
        Registers registers = new Registers(
                DeferredRegister.create(Registries.FLUID, namespace),
                DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, namespace),
                DeferredRegister.create(Registries.BLOCK, namespace),
                DeferredRegister.create(Registries.ITEM, namespace));
        registers.types().register(modBus);
        registers.fluids().register(modBus);
        registers.blocks().register(modBus);
        registers.items().register(modBus);
        return registers;
    }

    private static FluidSet registerFluid(Registers registers, String namespace, FluidSpec spec) {
        ResourceLocation still = spec.spent() ? LIQUID_HOT_STILL : LIQUID_STILL;
        ResourceLocation flow = spec.spent() ? LIQUID_HOT_FLOW : LIQUID_FLOW;
        String name = spec.name();

        DeferredHolder<FluidType, FluidType> type = registers.types().register(name, () ->
                new TexturedFluidType(spec.spent()
                        ? FluidType.Properties.create().descriptionId("fluid." + namespace + "." + name).temperature(spec.temperature())
                        : FluidType.Properties.create().descriptionId("fluid." + namespace + "." + name),
                        still, flow, spec.tint()).radiation(spec.radiation(), spec.spent()));

        Pending pending = new Pending();
        Supplier<BaseFlowingFluid.Properties> properties = Suppliers.memoize(() -> {
            BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(type, pending.source, pending.flowing)
                    .block(() -> (LiquidBlock) pending.block.get())
                    .bucket(pending.bucket);
            if (spec.metal())
                props.tickRate(METAL_TICK_RATE).levelDecreasePerBlock(METAL_LEVEL_DECREASE).slopeFindDistance(METAL_SLOPE_DISTANCE);
            return props;
        });

        pending.source = registers.fluids().<Fluid>register(name, () -> new BaseFlowingFluid.Source(properties.get()));
        pending.flowing = registers.fluids().<Fluid>register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties.get()));
        pending.block = registers.blocks().<Block>register(name, () -> {
            FlowingFluid source = (FlowingFluid) pending.source.get();
            BlockBehaviour.Properties blockProperties = BlockBehaviour.Properties.ofFullCopy(Blocks.WATER);
            return spec.spent() ? new SpentSaltLiquidBlock(source, blockProperties) : new LiquidBlock(source, blockProperties);
        });
        pending.bucket = registers.items().<Item>register(name + "_bucket", () -> {
            Item.Properties itemProperties = new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1);
            return spec.spent()
                    ? new SpentSaltBucketItem(pending.source.get(), itemProperties)
                    : new BucketItem(pending.source.get(), itemProperties);
        });
        return new FluidSet(pending.source, pending.bucket, pending.block);
    }

    private static final class Pending {
        Supplier<Fluid> source;
        Supplier<Fluid> flowing;
        Supplier<Block> block;
        Supplier<Item> bucket;
    }

    private static void addValidBlocks(BlockEntityTypeAddBlocksEvent event) {
        Block[] spentBlocks = LOADED.stream().filter(metal -> metal.spentSalt() != null)
                .map(metal -> metal.spentSalt().block().get()).toArray(Block[]::new);
        event.modify(CNBlockEntities.SPENT_SALT.get(), spentBlocks);
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        for (Path source : packSources) {
            event.addRepositorySource(consumer -> consumer.accept(createPack(source)));
        }
    }

    private static Pack createPack(Path source) {
        String fileName = source.getFileName().toString();
        PackLocationInfo info = new PackLocationInfo(CreateNuclearized.MODID + "_metals/" + fileName, Component.literal(fileName),
                PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier resources = Files.isDirectory(source)
                ? new DirectorySupplier(source)
                : new FilePackResources.FileResourcesSupplier(source);
        Pack.Metadata metadata = new Pack.Metadata(Component.literal("Create: Nuclearized metal pack"), PackCompatibility.COMPATIBLE,
                FeatureFlagSet.of(), List.of());
        return new Pack(info, resources, metadata, new PackSelectionConfig(true, Pack.Position.TOP, false));
    }

    private record DirectorySupplier(Path path) implements Pack.ResourcesSupplier {
        @Override
        public PackResources openPrimary(PackLocationInfo info) {
            return new PathPackResources(info, path);
        }

        @Override
        public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
            return new PathPackResources(info, path);
        }
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, path);
    }
}
