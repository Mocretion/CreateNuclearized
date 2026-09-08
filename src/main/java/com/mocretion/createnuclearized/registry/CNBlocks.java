package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.content.controlrod.ControlRodBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedChainDriveBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedCogwheelBlock;
import com.mocretion.createnuclearized.content.kinetics.ReactorEncasedShaftBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorFluidValveBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorBlock;
import com.mocretion.createnuclearized.content.reactor.ReactorState;
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

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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

    public static final BlockEntry<ReactorControllerBlock> REACTOR_CONTROLLER = REGISTRATE
            .block("reactor_controller", ReactorControllerBlock::new)
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
            .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT))
            .register();

    // Same idea as the encased shaft, but a cogwheel: encase a Create cogwheel with reactor casing Meshes rotation through the wall
    public static final BlockEntry<ReactorEncasedCogwheelBlock> REACTOR_CASING_ENCASED_COGWHEEL = REGISTRATE
            .block("reactor_casing_encased_cogwheel", p -> new ReactorEncasedCogwheelBlock(p, false, REACTOR_CASING::get))
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
            .transform(EncasingRegistry.addVariantTo(AllBlocks.COGWHEEL))
            .register();

    // Encased chain drive in reactor casing: relays rotation along the wall and links with Create's own chain drives
    // Placed directly (not by encasing), so it stays in the creative tab
    // Blockstate uses Create's ChainDriveGenerator pointed at our hand authored models; item model is hand authored
    public static final BlockEntry<ReactorEncasedChainDriveBlock> REACTOR_ENCASED_CHAIN_DRIVE = REGISTRATE
            .block("reactor_encased_chain_drive", ReactorEncasedChainDriveBlock::new)
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
            .initialProperties(() -> Blocks.QUARTZ_BLOCK)
            .properties(p -> p.mapColor(MapColor.QUARTZ)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(),
                    p.models().cubeAll(c.getName(), ResourceLocation.withDefaultNamespace("block/quartz_block_side"))))
            .simpleItem()
            .register();

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
