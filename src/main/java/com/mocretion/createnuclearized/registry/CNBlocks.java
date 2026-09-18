package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.content.boiler.ContainedBlazeBurnerBlock;
import com.mocretion.createnuclearized.content.boiler.ExhaustHoodBlock;
import com.mocretion.createnuclearized.content.controlrod.ControlRodBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedChainDriveBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedCogwheelBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedShaftBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorFluidValveBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorState;
import com.mocretion.createnuclearized.content.turbine.SteamInputValveBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineControllerBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorBlockItem;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorStructuralBlock;
import com.mocretion.createnuclearized.content.turbine.TurbineGeneratorBlock;
import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveGenerator;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntry;

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
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public final class CNBlocks {

    // Stress the control rod draws per RPM, deployer tier Also what its tooltip shows
    private static final double CONTROL_ROD_IMPACT = 4.0;

    // RPM shown in the generators tooltip. Real capacity scales with blade count in the block entity Provisional
    private static final int GENERATOR_RPM = 160;

    public static final BlockEntry<ReactorControllerBlock> REACTOR_CONTROLLER = REGISTRATE
            .block("reactor_controller", ReactorControllerBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            // FACING face shows the status tile (from reactor_controller_states.png, picked by STATE); the other five are casing
            // FACING rotates like a vanilla observer/dispenser, with the panel authored on the base models north face
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
            // Item uses the stopped block model
            .item()
            .model((c, p) -> p.withExistingParent(c.getName(), cn("block/reactor_controller")))
            .build()
            .register();

    // Solid casing, the bulk of the walls Connected texture
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
            .onRegister(CreateRegistrate.connectedTextures(() -> new SimpleCTBehaviour(CNSpriteShifts.REACTOR_CASING)))
            .simpleItem()
            .register();

    // Transparent casing for windows into the core (glass texture for now)
    // Has to be TransparentBlock (the vanilla glass class), not a plain Block, or it dims skylight instead of passing it through
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

    // Shell port: cold molten salt gets pumped in here Magma placeholder texture
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

    // Shell port in the floor: hot spent salt drains out here Redstone block placeholder texture
    public static final BlockEntry<ReactorFluidValveBlock> HEATED_SALT_OUTPUT_VALVE = REGISTRATE
            .block("heated_salt_output_valve", p -> new ReactorFluidValveBlock(p, false))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("Depleted Salt Output Valve")
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_RED)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/redstone_block"))))
            .simpleItem()
            .register();

    // A Create shaft encased in reactor casing
    // Registered as an encasable variant of Create's shaft, so right clicking a shaft with reactor casing gives you this
    // Counts as a shell member and carries rotation through the wall
    public static final BlockEntry<ReactorEncasedShaftBlock> REACTOR_CASING_ENCASED_SHAFT = REGISTRATE
            .block("reactor_casing_encased_shaft", p -> new ReactorEncasedShaftBlock(p, REACTOR_CASING::get))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            // noOcclusion, or the interior samples no light and the shaft renders black
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            // Model and item are hand authored JSON (children of Create's encased shaft templates with our casing); we only generate the
            // axis rotated blockstate pointing at them
            .blockstate((c, p) -> {
                ModelFile model = new ModelFile.UncheckedModelFile(cn("block/reactor_casing_encased_shaft"));
                p.getVariantBuilder(c.getEntry()).forAllStates(s -> axisModel(model, s, true));
            })
            .item()
            .model((c, p) -> {}) // hand authored item model
            // Only obtainable by encasing a shaft, so keep it out of the creative inventory
            .removeTab(CreateNuclearized.MAIN_TAB_KEY)
            .build()
            // Drop the plain shaft back like Create's own encased shaft, not the never craftable encased item
            .loot((lt, b) -> lt.dropOther(b, AllBlocks.SHAFT.get()))
            .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT))
            .register();

    // Same idea as the encased shaft, but a cogwheel: encase a Create cogwheel with reactor casing Meshes rotation through the wall
    public static final BlockEntry<ReactorEncasedCogwheelBlock> REACTOR_CASING_ENCASED_COGWHEEL = REGISTRATE
            .block("reactor_casing_encased_cogwheel", p -> new ReactorEncasedCogwheelBlock(p, false, REACTOR_CASING::get))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            // noOcclusion, or the cog renders black (interior samples no light)
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            // Hand authored JSON like the shaft
            // top_shaft/bottom_shaft picks the shell variant thats open on that axle end, like Create; the spinning cog comes from the
            // block entity renderer
            .blockstate((c, p) -> {
                ModelFile base = new ModelFile.UncheckedModelFile(cn("block/reactor_casing_encased_cogwheel"));
                ModelFile top = new ModelFile.UncheckedModelFile(cn("block/reactor_casing_encased_cogwheel_top"));
                ModelFile bottom = new ModelFile.UncheckedModelFile(cn("block/reactor_casing_encased_cogwheel_bottom"));
                ModelFile topBottom = new ModelFile.UncheckedModelFile(cn("block/reactor_casing_encased_cogwheel_top_bottom"));
                p.getVariantBuilder(c.getEntry()).forAllStates(s -> {
                    boolean t = s.getValue(EncasedCogwheelBlock.TOP_SHAFT);
                    boolean b = s.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT);
                    ModelFile model = t && b ? topBottom : t ? top : b ? bottom : base;
                    return axisModel(model, s, false);
                });
            })
            .item()
            .model((c, p) -> {}) // hand authored item model
            // Only obtainable by encasing a cogwheel, so keep it out of the creative inventory
            .removeTab(CreateNuclearized.MAIN_TAB_KEY)
            .build()
            // Drop the plain cogwheel back like Create's own encased cogwheel, not the never craftable encased item
            .loot((lt, b) -> lt.dropOther(b, AllBlocks.COGWHEEL.get()))
            .transform(EncasingRegistry.addVariantTo(AllBlocks.COGWHEEL))
            .register();

    // Encased chain drive in reactor casing: relays rotation along the wall and links with Create's own chain drives
    // Placed directly (not by encasing), so it stays in the creative tab
    // Blockstate uses Create's ChainDriveGenerator pointed at our hand authored models; item model is hand authored
    public static final BlockEntry<ReactorEncasedChainDriveBlock> REACTOR_ENCASED_CHAIN_DRIVE = REGISTRATE
            .block("reactor_encased_chain_drive", ReactorEncasedChainDriveBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            // noOcclusion, or the shaft ends render black
            .properties(p -> p.mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> new ChainDriveGenerator((state, suffix) ->
                    new ModelFile.UncheckedModelFile(cn("block/reactor_encased_chain_drive/" + suffix)))
                    .generate(c, p))
            .item()
            .model((c, p) -> {}) // hand authored item model
            .build()
            .register();

    // Control rod: takes a top face casing slot
    // Looks and drives like a fixed vertical axis encased cogwheel (shaft on top, cog meshing on the sides); turning it slides the rod down
    // into the core (visual only for now)
    // Shell is the hand authored block/control_rod model; the cog, shaft nub, and sliding rod come from ControlRodRenderer
    public static final BlockEntry<ControlRodBlock> CONTROL_ROD = REGISTRATE
            .block("control_rod", ControlRodBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            // noOcclusion, or the cog and shaft render black
            .properties(p -> p.mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    new ModelFile.UncheckedModelFile(cn("block/control_rod"))))
            // Kinetic stress impact, so the BE draws stress and the item shows the line
            .onRegister(b -> BlockStressValues.IMPACTS.register(b, () -> CONTROL_ROD_IMPACT))
            .item()
            .model((c, p) -> {}) // hand authored item model
            .build()
            .register();

    // Monitor: a shell block (any number, anywhere in the wall) that opens a read only telemetry screen on right click
    // Sea lantern placeholder texture
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

    // Graphite moderator (coal block placeholder). When it bridges two channels it adds ReactorModerator.GRAPHITE_RATE mB/tick to each
    // Valid interior block
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

    // Beryllium moderator (end stone bricks placeholder) Stronger than graphite (ReactorModerator.BERYLLIUM_RATE mB/tick)
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

    // Xenon cooler (quartz placeholder)
    // A rate cooler: on a moderators face it adds mB/tick to the channels that moderator bridges (see ReactorModifier)
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

    // Turbine multiblock (steam turbine) All placeholder textures for now, real art and connected textures come later

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

    // Rotation source: sits in the center of a square end wall and drives the shaft and rotors on the turbine axis
    public static final BlockEntry<TurbineGeneratorBlock> TURBINE_GENERATOR = REGISTRATE
            .block("turbine_generator", TurbineGeneratorBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/copper_block"))))
            // Kinetic generator: speed and stress capacity both come from the blades (see the block entity)
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

    // Windowed casing Same light gotcha as reactor_glass, so TransparentBlock not a plain Block
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

    // Steam input valve: the player pipes pressurized steam in here, the running turbine drains it
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

    // Water wheel style rotors that replace the blade square: one placed block claims an NxN plane with structural fillers
    // Passive (just rotates, the generator drives it); the center carries the built in shaft on the axis
    // Small uses Create's water wheel model, medium its large water wheel, large the player authored turbine_large model

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
            .model((c, p) -> {}) // hand authored item model
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
            .model((c, p) -> {}) // hand authored item model
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
            .model((c, p) -> {}) // hand authored item model
            .build()
            .register();

    // Invisible solid filler that gives the large rotor its 3x3 hitbox; no item, drops nothing (breaking one breaks the whole rotor)
    // Only the large rotor uses fillers, so name it after that rotor for JADE and pick block
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
            // Empty table; breaking a filler breaks the whole rotor, which drops the rotor item (survival only)
            .loot((lt, b) -> lt.add(b, net.minecraft.world.level.storage.loot.LootTable.lootTable()))
            .register();

    // Static diaphragm, the other stage half; steam passes through it onto the next blades
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

    // Steam plant: a blaze burner wearing a hazmat helmet, cooling reactor salt to boil water into steam

    // Made by right clicking a lit blaze burner with a hazmat helmet Reuses Create's brazier model; the blaze + helmet come from the BER
    public static final BlockEntry<ContainedBlazeBurnerBlock> CONTAINED_BLAZE_BURNER = REGISTRATE
            .block("contained_blaze_burner", ContainedBlazeBurnerBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            // noOcclusion + glow like a lit blaze burner (the brazier isnt a full cube)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(s -> 13)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
            // Same as Create's blaze burner, or the cage gaps render solid black
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), new ModelFile.UncheckedModelFile(
                    ResourceLocation.fromNamespaceAndPath("create", "block/blaze_burner/block"))))
            .item()
            .model((c, p) -> {}) // hand authored item model
            .build()
            .register();

    // Sits a couple blocks above the basin and catches the rising steam into a small pumpable buffer Copper block placeholder
    public static final BlockEntry<ExhaustHoodBlock> EXHAUST_HOOD = REGISTRATE
            .block("exhaust_hood", ExhaustHoodBlock::new)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/copper_block"))))
            .simpleItem()
            .register();

    /** Any of the three turbine rotor sizes */
    public static boolean isTurbineRotor(BlockState state) {
        return state.is(TURBINE_ROTOR.get()) || state.is(MEDIUM_TURBINE_ROTOR.get()) || state.is(LARGE_TURBINE_ROTOR.get());
    }

    private CNBlocks() {}

    /** A ResourceLocation in this mods namespace */
    private static ResourceLocation cn(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, path);
    }

    /**
     * The model rotated to the blocks axis, for the encased shaft/cogwheel (base models authored along Y)
     * X and Z reuse the same model, just rotated
     */
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

    /**
     * Full cube controller model
     * The north face shows a 16x16 tile of the 32x32 reactor_controller_states sheet, picked by the (u, v) offset in uv units (sheet is
     * 32px, so 16px == 8 uv units); the other five faces are casing. facingModel then rotates north to FACING
     */
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

    /** The controller model rotated so its north (panel) face points in dir. Same rotation convention as the vanilla observer/dispenser */
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

    /** No op that forces this class to load so the fields above initialize */
    public static void register() {}
}
