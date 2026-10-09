package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.boiler.ContainedBlazeBurnerBlock;
import com.mocretion.createnuclearized.content.boiler.EvaporationBasinBlock;
import com.mocretion.createnuclearized.content.boiler.ExhaustHoodBlock;
import com.mocretion.createnuclearized.content.controlrod.ControlRodBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedChainDriveBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedCogwheelBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedShaftBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorFluidValveBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorBlock;
import com.mocretion.createnuclearized.content.turbine.SteamInputValveBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineControllerBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineGeneratorBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorBlockItem;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorStructuralBlock;
import com.mocretion.createnuclearized.content.unmixer.PrecisionUnmixerBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveGenerator;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;

public final class CNBlocks {

    private static final double CONTROL_ROD_IMPACT = 4.0;
    private static final double PRECISION_UNMIXER_IMPACT = 4.0;
    private static final int GENERATOR_RPM = 160;

    public static final BlockEntry<ReactorControllerBlock> REACTOR_CONTROLLER = REGISTRATE
            .block("reactor_controller", ReactorControllerBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> {
                ModelFile stopped = controllerModel(p, "reactor_controller", 0, 0);
                ModelFile idle = controllerModel(p, "reactor_controller_idle", 8, 0);
                ModelFile running = controllerModel(p, "reactor_controller_running", 0, 8);
                p.getVariantBuilder(c.getEntry()).forAllStates(s -> {
                    ModelFile model = switch (s.getValue(ReactorControllerBlock.STATE)) {
                        case STOPPED -> stopped;
                        case IDLE -> idle;
                        case RUNNING -> running;
                    };
                    return facingModel(model, s.getValue(ReactorControllerBlock.FACING));
                });
            })
            .item()
            .model((c, p) -> p.withExistingParent(c.getName(), cn("block/reactor_controller")))
            .build()
            .register();

    public static final BlockEntry<Block> REACTOR_CASING = REGISTRATE
            .block("reactor_casing", Block::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(),
                            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/reactor_casing"))))
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(CNSpriteShifts.REACTOR_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((b, cc) -> cc.makeCasing(b, CNSpriteShifts.REACTOR_CASING)))
            .simpleItem()
            .register();

    public static final BlockEntry<TransparentBlock> REACTOR_GLASS = REGISTRATE
            .block("reactor_glass", TransparentBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.GLASS)
            .properties(p -> p.mapColor(MapColor.NONE)
                    .sound(SoundType.GLASS)
                    .noOcclusion())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(),
                            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/reactor_glass"))
                            .renderType("cutout")))
            .onRegister(CreateRegistrate.connectedTextures(() -> new SimpleCTBehaviour(CNSpriteShifts.REACTOR_GLASS)))
            .simpleItem()
            .register();

    public static final BlockEntry<ReactorFluidValveBlock> MOLTEN_SALT_INPUT_VALVE = REGISTRATE
            .block("molten_salt_input_valve", p -> new ReactorFluidValveBlock(p, true))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Fissile Salt Input Valve")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/magma"))))
            .simpleItem()
            .register();

    public static final BlockEntry<ReactorFluidValveBlock> HEATED_SALT_OUTPUT_VALVE = REGISTRATE
            .block("heated_salt_output_valve", p -> new ReactorFluidValveBlock(p, false))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Spent Salt Output Valve")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_RED)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/redstone_block"))))
            .simpleItem()
            .register();

    public static final BlockEntry<ReactorEncasedShaftBlock> REACTOR_CASING_ENCASED_SHAFT = REGISTRATE
            .block("reactor_casing_encased_shaft", p -> new ReactorEncasedShaftBlock(p, REACTOR_CASING::get))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> {
                ModelFile model = new ModelFile.UncheckedModelFile(cn("block/reactor_casing_encased_shaft"));
                p.getVariantBuilder(c.getEntry()).forAllStates(s -> axisModel(model, s, true));
            })
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(CNSpriteShifts.REACTOR_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((b, cc) -> cc.make(b, CNSpriteShifts.REACTOR_CASING,
                    (state, dir) -> dir.getAxis() != state.getValue(RotatedPillarKineticBlock.AXIS))))
            .item()
            .model((c, p) -> {})
            .removeTab(CreateNuclearized.MAIN_TAB_KEY)
            .build()
            .loot((lt, b) -> lt.dropOther(b, AllBlocks.SHAFT.get()))
            .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT))
            .register();

    public static final BlockEntry<ReactorEncasedCogwheelBlock> REACTOR_CASING_ENCASED_COGWHEEL = REGISTRATE
            .block("reactor_casing_encased_cogwheel", p -> new ReactorEncasedCogwheelBlock(p, false, REACTOR_CASING::get))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate(CNBlocks::encasedCogwheelState)
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCogCTBehaviour(CNSpriteShifts.REACTOR_CASING,
                    Couple.create(CNSpriteShifts.REACTOR_COGWHEEL_SIDE, CNSpriteShifts.REACTOR_COGWHEEL_OTHERSIDE))))
            .onRegister(CreateRegistrate.casingConnectivity((b, cc) -> cc.make(b, CNSpriteShifts.REACTOR_CASING, CNBlocks::cogCasingFace)))
            .item()
            .model((c, p) -> {})
            .removeTab(CreateNuclearized.MAIN_TAB_KEY)
            .build()
            .loot((lt, b) -> lt.dropOther(b, AllBlocks.COGWHEEL.get()))
            .transform(EncasingRegistry.addVariantTo(AllBlocks.COGWHEEL))
            .register();

    public static final BlockEntry<ReactorEncasedCogwheelBlock> REACTOR_CASING_ENCASED_LARGE_COGWHEEL = REGISTRATE
            .block("reactor_casing_encased_large_cogwheel", p -> new ReactorEncasedCogwheelBlock(p, true, REACTOR_CASING::get))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate(CNBlocks::encasedCogwheelState)
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCogCTBehaviour(CNSpriteShifts.REACTOR_CASING)))
            .onRegister(CreateRegistrate.casingConnectivity((b, cc) -> cc.make(b, CNSpriteShifts.REACTOR_CASING, CNBlocks::cogCasingFace)))
            .item()
            .model((c, p) -> {})
            .removeTab(CreateNuclearized.MAIN_TAB_KEY)
            .build()
            .loot((lt, b) -> lt.dropOther(b, AllBlocks.LARGE_COGWHEEL.get()))
            .transform(EncasingRegistry.addVariantTo(AllBlocks.LARGE_COGWHEEL))
            .register();

    public static final BlockEntry<ReactorEncasedChainDriveBlock> REACTOR_ENCASED_CHAIN_DRIVE = REGISTRATE
            .block("reactor_encased_chain_drive", ReactorEncasedChainDriveBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) ->
                    new ModelFile.UncheckedModelFile(cn("block/reactor_encased_chain_drive/" + suffix)))
                    .generate(c, p))
            .item()
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<ControlRodBlock> CONTROL_ROD = REGISTRATE
            .block("control_rod", ControlRodBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    new ModelFile.UncheckedModelFile(cn("block/control_rod"))))
            .onRegister(b -> BlockStressValues.IMPACTS.register(b, () -> CONTROL_ROD_IMPACT))
            .item()
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<PrecisionUnmixerBlock> PRECISION_UNMIXER = REGISTRATE
            .block("precision_unmixer", PrecisionUnmixerBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.PODZOL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    new ModelFile.UncheckedModelFile(cn("block/unmixer/block"))))
            .onRegister(b -> BlockStressValues.IMPACTS.register(b, () -> PRECISION_UNMIXER_IMPACT))
            .item(AssemblyOperatorBlockItem::new)
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<ReactorMonitorBlock> REACTOR_MONITOR = REGISTRATE
            .block("reactor_monitor", ReactorMonitorBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/sea_lantern"))))
            .simpleItem()
            .register();

    public static final BlockEntry<Block> GRAPHITE_BLOCK = REGISTRATE
            .block("graphite_block", Block::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.COAL_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/coal_block"))))
            .simpleItem()
            .register();

    public static final BlockEntry<Block> BERYLLIUM_BLOCK = REGISTRATE
            .block("beryllium_block", Block::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.END_STONE_BRICKS)
            .properties(p -> p.mapColor(MapColor.SAND)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/end_stone_bricks"))))
            .simpleItem()
            .register();

    public static final BlockEntry<Block> XENON_BLOCK = REGISTRATE
            .block("xenon_block", Block::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.QUARTZ_BLOCK)
            .properties(p -> p.mapColor(MapColor.QUARTZ)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/quartz_block_side"))))
            .simpleItem()
            .register();

    public static final BlockEntry<TurbineControllerBlock> TURBINE_CONTROLLER = REGISTRATE
            .block("turbine_controller", TurbineControllerBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/chiseled_stone_bricks"))))
            .simpleItem()
            .register();

    public static final BlockEntry<TurbineGeneratorBlock> TURBINE_GENERATOR = REGISTRATE
            .block("turbine_generator", TurbineGeneratorBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/copper_block"))))
            .onRegister(BlockStressValues.setGeneratorSpeed(GENERATOR_RPM, true))
            .simpleItem()
            .register();

    public static final BlockEntry<Block> TURBINE_CASING = REGISTRATE
            .block("turbine_casing", Block::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/iron_block"))))
            .simpleItem()
            .register();

    public static final BlockEntry<TransparentBlock> TURBINE_GLASS = REGISTRATE
            .block("turbine_glass", TransparentBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.GLASS)
            .properties(p -> p.mapColor(MapColor.NONE)
                    .sound(SoundType.GLASS)
                    .noOcclusion())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/glass"))
                            .renderType("cutout")))
            .simpleItem()
            .register();

    public static final BlockEntry<SteamInputValveBlock> STEAM_INPUT_VALVE = REGISTRATE
            .block("steam_input_valve", SteamInputValveBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/light_blue_concrete"))))
            .simpleItem()
            .register();

    public static final BlockEntry<TurbineRotorBlock> TURBINE_ROTOR = REGISTRATE
            .block("turbine_rotor", p -> new TurbineRotorBlock(p, 1, 0))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Turbine Rotor")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStates(s ->
                    axisModel(new ModelFile.UncheckedModelFile(cn("block/turbines/turbine_3/block")), s, false)))
            .item((b, p) -> new TurbineRotorBlockItem(b, p))
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<TurbineRotorBlock> MEDIUM_TURBINE_ROTOR = REGISTRATE
            .block("medium_turbine_rotor", p -> new TurbineRotorBlock(p, 2, 0))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Medium Turbine Rotor")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStates(s ->
                    axisModel(new ModelFile.UncheckedModelFile(cn("block/turbines/turbine_5/block")), s, false)))
            .item((b, p) -> new TurbineRotorBlockItem(b, p))
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<TurbineRotorBlock> LARGE_TURBINE_ROTOR = REGISTRATE
            .block("large_turbine_rotor", p -> new TurbineRotorBlock(p, 3, 1))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Large Turbine Rotor")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStates(s ->
                    axisModel(new ModelFile.UncheckedModelFile(cn("block/turbines/turbine_7/block")), s, false)))
            .item((b, p) -> new TurbineRotorBlockItem(b, p))
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<TurbineRotorStructuralBlock> TURBINE_ROTOR_STRUCTURAL = REGISTRATE
            .block("turbine_rotor_structural", TurbineRotorStructuralBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Large Turbine Rotor")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), p.models().getBuilder(c.getName())))
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()))
            .register();

    public static final BlockEntry<Block> TURBINE_DIAPHRAGM = REGISTRATE
            .block("turbine_diaphragm", Block::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/gray_concrete"))))
            .simpleItem()
            .register();

    public static final BlockEntry<ContainedBlazeBurnerBlock> CONTAINED_BLAZE_BURNER = REGISTRATE
            .block("contained_blaze_burner", ContainedBlazeBurnerBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(s -> 13)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), new ModelFile.UncheckedModelFile(
                    ResourceLocation.fromNamespaceAndPath("create", "block/blaze_burner/block"))))
            .item()
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<EvaporationBasinBlock> EVAPORATION_BASIN = REGISTRATE
            .block("evaporation_basin", EvaporationBasinBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), new ModelFile.UncheckedModelFile(
                    ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/evaporation_basin"))))
            .item()
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<ExhaustHoodBlock> EXHAUST_HOOD = REGISTRATE
            .block("exhaust_hood", ExhaustHoodBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), new ModelFile.UncheckedModelFile(
                    ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/exhaust_hood"))))
            .item()
            .model((c, p) -> {})
            .build()
            .register();

    public static final BlockEntry<Block> THORIUM_ORE = REGISTRATE
            .block("thorium_ore", Block::new)
            .initialProperties(() -> Blocks.GOLD_ORE)
            .properties(p -> p.mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), cn("block/thorium_ore"))))
            .loot((lt, b) -> lt.add(b, lt.createOreDrop(b, CNItems.RAW_THORIUM.get())))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES, Tags.Blocks.ORES_IN_GROUND_STONE,
                    Tags.Blocks.ORE_RATES_SINGULAR, CNTags.THORIUM_ORES)
            .item()
            .tag(Tags.Items.ORES, Tags.Items.ORES_IN_GROUND_STONE, Tags.Items.ORE_RATES_SINGULAR, CNTags.THORIUM_ORE_ITEMS)
            .build()
            .register();

    public static final BlockEntry<Block> DEEPSLATE_THORIUM_ORE = REGISTRATE
            .block("deepslate_thorium_ore", Block::new)
            .initialProperties(() -> Blocks.DEEPSLATE_GOLD_ORE)
            .properties(p -> p.mapColor(MapColor.DEEPSLATE)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), cn("block/deepslate_thorium_ore"))))
            .loot((lt, b) -> lt.add(b, lt.createOreDrop(b, CNItems.RAW_THORIUM.get())))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE,
                    Tags.Blocks.ORE_RATES_SINGULAR, CNTags.THORIUM_ORES)
            .item()
            .tag(Tags.Items.ORES, Tags.Items.ORES_IN_GROUND_DEEPSLATE, Tags.Items.ORE_RATES_SINGULAR, CNTags.THORIUM_ORE_ITEMS)
            .build()
            .register();

    public static final BlockEntry<Block> SCULK_URANIUM_ORE = REGISTRATE
            .block("sculk_uranium_ore", Block::new)
            .initialProperties(() -> Blocks.GOLD_ORE)
            .properties(p -> p.mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.SCULK)
                    .lightLevel(s -> 3)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), cn("block/sculk_uranium"))))
            .loot((lt, b) -> lt.add(b, lt.createOreDrop(b, CNItems.RAW_URANIUM.get())))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES, Tags.Blocks.ORE_RATES_SINGULAR,
                    CNTags.URANIUM_ORES)
            .item()
            .tag(Tags.Items.ORES, Tags.Items.ORE_RATES_SINGULAR, CNTags.URANIUM_ORE_ITEMS)
            .build()
            .register();

    public static boolean isTurbineRotor(BlockState state) {
        return state.is(TURBINE_ROTOR.get()) || state.is(MEDIUM_TURBINE_ROTOR.get()) || state.is(LARGE_TURBINE_ROTOR.get());
    }

    private CNBlocks() {}

    private static ResourceLocation cn(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, path);
    }

    private static ConfiguredModel[] axisModel(ModelFile model, BlockState state, boolean uvlock) {
        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        int rotX = axis == Direction.Axis.Y ? 0 : 90;
        int rotY = switch (axis) {
            case X -> 90;
            case Z -> 180;
            default -> 0;
        };
        return ConfiguredModel.builder().modelFile(model).rotationX(rotX).rotationY(rotY).uvLock(uvlock).build();
    }

    private static <T extends Block> void encasedCogwheelState(DataGenContext<Block, T> c, RegistrateBlockstateProvider p) {
        p.getVariantBuilder(c.getEntry()).forAllStates(s -> {
            String suffix = (s.getValue(EncasedCogwheelBlock.TOP_SHAFT) ? "_top" : "")
                    + (s.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT) ? "_bottom" : "");
            return axisModel(new ModelFile.UncheckedModelFile(cn("block/" + c.getName() + suffix)), s, false);
        });
    }

    private static boolean cogCasingFace(BlockState state, Direction face) {
        if (face.getAxis() != state.getValue(RotatedPillarKineticBlock.AXIS)) return false;
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        return !state.getValue(positive ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT);
    }

    private static ModelFile controllerModel(RegistrateBlockstateProvider p, String name, int u, int v) {
        ResourceLocation side = cn("block/reactor_casing");
        ResourceLocation front = cn("block/reactor_controller_states");
        return p.models().withExistingParent(name, ResourceLocation.withDefaultNamespace("block/block"))
                .texture("particle", side)
                .texture("side", side)
                .texture("front", front)
                .element()
                .from(0, 0, 0).to(16, 16, 16)
                .face(Direction.NORTH).texture("#front").uvs(u, v, u + 8, v + 8).cullface(Direction.NORTH).end()
                .face(Direction.SOUTH).texture("#side").uvs(0, 0, 16, 16).cullface(Direction.SOUTH).end()
                .face(Direction.EAST).texture("#side").uvs(0, 0, 16, 16).cullface(Direction.EAST).end()
                .face(Direction.WEST).texture("#side").uvs(0, 0, 16, 16).cullface(Direction.WEST).end()
                .face(Direction.UP).texture("#side").uvs(0, 0, 16, 16).cullface(Direction.UP).end()
                .face(Direction.DOWN).texture("#side").uvs(0, 0, 16, 16).cullface(Direction.DOWN).end()
                .end();
    }

    private static ConfiguredModel[] facingModel(ModelFile model, Direction dir) {
        int rotX = switch (dir) {
            case DOWN -> 90;
            case UP -> 270;
            default -> 0;
        };
        int rotY = switch (dir) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
        return ConfiguredModel.builder().modelFile(model).rotationX(rotX).rotationY(rotY).build();
    }

    public static void register() {}
}
