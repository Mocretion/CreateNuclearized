package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.content.controlrod.ControlRodBlockEntity;
import com.mocretion.createnuclearized.content.controlrod.ControlRodRenderer;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerBlockEntity;
import com.mocretion.createnuclearized.content.reactor.ReactorFluidValveBlockEntity;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public final class CNBlockEntities {

    public static final BlockEntityEntry<ReactorControllerBlockEntity> REACTOR_CONTROLLER = REGISTRATE
            .blockEntity("reactor_controller", ReactorControllerBlockEntity::new)
            .validBlock(CNBlocks.REACTOR_CONTROLLER)
            .register();

    // Backs both molten salt input and heated salt output valves
    public static final BlockEntityEntry<ReactorFluidValveBlockEntity> REACTOR_FLUID_VALVE = REGISTRATE
            .blockEntity("reactor_fluid_valve", ReactorFluidValveBlockEntity::new)
            .validBlocks(CNBlocks.MOLTEN_SALT_INPUT_VALVE, CNBlocks.HEATED_SALT_OUTPUT_VALVE)
            .register();

    public static final BlockEntityEntry<ReactorMonitorBlockEntity> REACTOR_MONITOR = REGISTRATE
            .blockEntity("reactor_monitor", ReactorMonitorBlockEntity::new)
            .validBlock(CNBlocks.REACTOR_MONITOR)
            .register();

    public static final BlockEntityEntry<ControlRodBlockEntity> CONTROL_ROD = REGISTRATE
            .blockEntity("control_rod", ControlRodBlockEntity::new)
            .validBlock(CNBlocks.CONTROL_ROD)
            // The renderer lambda body only runs on the client
            .renderer(() -> ControlRodRenderer::new)
            .register();

    // Encased shaft: a plain Create KineticBlockEntity bound to our block so it counts as a shell member
    public static final BlockEntityEntry<KineticBlockEntity> REACTOR_ENCASED_SHAFT = REGISTRATE
            .blockEntity("reactor_encased_shaft", KineticBlockEntity::new)
            // Draw the spinning shaft through the casing
            // .visual comes from CreateBlockEntityBuilder, so it has to be chained before .validBlock (which drops back to the plain
            // builder)
            .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT))
            .validBlock(CNBlocks.REACTOR_CASING_ENCASED_SHAFT)
            .renderer(() -> ShaftRenderer::new)
            .register();

    // Encased cogwheel: a Create SimpleKineticBlockEntity relay bound to our block
    public static final BlockEntityEntry<SimpleKineticBlockEntity> REACTOR_ENCASED_COGWHEEL = REGISTRATE
            .blockEntity("reactor_encased_cogwheel", SimpleKineticBlockEntity::new)
            .visual(() -> EncasedCogVisual::small)
            .validBlock(CNBlocks.REACTOR_CASING_ENCASED_COGWHEEL)
            .renderer(() -> EncasedCogRenderer::small)
            .register();

    // Encased chain drive: a plain KineticBlockEntity relay
    // Renders the shaft ends the same way Create's chain drive does (it reuses Create's ENCASED_SHAFT type)
    public static final BlockEntityEntry<KineticBlockEntity> REACTOR_ENCASED_CHAIN_DRIVE = REGISTRATE
            .blockEntity("reactor_encased_chain_drive", KineticBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT))
            .validBlock(CNBlocks.REACTOR_ENCASED_CHAIN_DRIVE)
            .renderer(() -> ShaftRenderer::new)
            .register();

    private CNBlockEntities() {}

    /** No op that forces this class to load so the fields above initialize */
    public static void register() {}
}
