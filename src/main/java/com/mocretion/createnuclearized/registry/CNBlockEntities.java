package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.content.boiler.IndustrialHeaterBlockEntity;
import com.mocretion.createnuclearized.content.boiler.EvaporationBasinBlockEntity;
import com.mocretion.createnuclearized.content.boiler.EvaporationBasinRenderer;
import com.mocretion.createnuclearized.content.boiler.ExhaustHoodBlockEntity;
import com.mocretion.createnuclearized.content.controlrod.ControlRodBlockEntity;
import com.mocretion.createnuclearized.content.controlrod.ControlRodRenderer;
import com.mocretion.createnuclearized.content.meteringvalve.MeteringValveBlockEntity;
import com.mocretion.createnuclearized.content.meteringvalve.MeteringValveRenderer;
import com.mocretion.createnuclearized.content.fluid.SpentSaltBlockEntity;
import com.mocretion.createnuclearized.content.kinetics.RtgBlockEntity;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerBlockEntity;
import com.mocretion.createnuclearized.content.reactor.ReactorFluidValveBlockEntity;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorBlockEntity;
import com.mocretion.createnuclearized.content.turbine.TurbineControllerBlockEntity;
import com.mocretion.createnuclearized.content.turbine.TurbineGeneratorBlockEntity;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorBlockEntity;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorRenderer;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorVisual;
import com.mocretion.createnuclearized.content.turbine.TurbineSteamInputValveBlockEntity;
import com.mocretion.createnuclearized.content.unmixer.PrecisionUnmixerBlockEntity;
import com.mocretion.createnuclearized.content.unmixer.PrecisionUnmixerRenderer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.world.level.block.Block;

public final class CNBlockEntities {

    public static final BlockEntityEntry<ReactorControllerBlockEntity> REACTOR_CONTROLLER = REGISTRATE
            .blockEntity("reactor_controller", ReactorControllerBlockEntity::new)
            .validBlock(CNBlocks.REACTOR_CONTROLLER)
            .register();

    public static final BlockEntityEntry<ReactorFluidValveBlockEntity> REACTOR_FLUID_VALVE = REGISTRATE
            .blockEntity("reactor_fluid_valve", ReactorFluidValveBlockEntity::new)
            .validBlocks(CNBlocks.REACTOR_SALT_INPUT_VALVE, CNBlocks.REACTOR_SALT_OUTPUT_VALVE)
            .register();

    public static final BlockEntityEntry<ReactorMonitorBlockEntity> REACTOR_MONITOR = REGISTRATE
            .blockEntity("reactor_monitor", ReactorMonitorBlockEntity::new)
            .validBlock(CNBlocks.REACTOR_MONITOR)
            .register();

    public static final BlockEntityEntry<ControlRodBlockEntity> CONTROL_ROD = REGISTRATE
            .blockEntity("control_rod", ControlRodBlockEntity::new)
            .validBlock(CNBlocks.CONTROL_ROD)
            .renderer(() -> ControlRodRenderer::new)
            .register();

    public static final BlockEntityEntry<KineticBlockEntity> REACTOR_ENCASED_SHAFT = REGISTRATE
            .blockEntity("reactor_encased_shaft", KineticBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT))
            .validBlock(CNBlocks.REACTOR_CASING_ENCASED_SHAFT)
            .renderer(() -> ShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<SimpleKineticBlockEntity> REACTOR_ENCASED_COGWHEEL = REGISTRATE
            .blockEntity("reactor_encased_cogwheel", SimpleKineticBlockEntity::new)
            .visual(() -> EncasedCogVisual::small)
            .validBlock(CNBlocks.REACTOR_CASING_ENCASED_COGWHEEL)
            .renderer(() -> EncasedCogRenderer::small)
            .register();

    public static final BlockEntityEntry<SimpleKineticBlockEntity> REACTOR_ENCASED_LARGE_COGWHEEL = REGISTRATE
            .blockEntity("reactor_encased_large_cogwheel", SimpleKineticBlockEntity::new)
            .visual(() -> EncasedCogVisual::large)
            .validBlock(CNBlocks.REACTOR_CASING_ENCASED_LARGE_COGWHEEL)
            .renderer(() -> EncasedCogRenderer::large)
            .register();

    public static final BlockEntityEntry<RtgBlockEntity> RTG = REGISTRATE
            .blockEntity("rtg", RtgBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT))
            .validBlocks(CNBlocks.PLUTONIUM_RTG, CNBlocks.AMERICIUM_RTG)
            .renderer(() -> ShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<KineticBlockEntity> REACTOR_ENCASED_CHAIN_DRIVE = REGISTRATE
            .blockEntity("reactor_encased_chain_drive", KineticBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT))
            .validBlock(CNBlocks.REACTOR_ENCASED_CHAIN_DRIVE)
            .renderer(() -> ShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<TurbineControllerBlockEntity> TURBINE_CONTROLLER = REGISTRATE
            .blockEntity("turbine_controller", TurbineControllerBlockEntity::new)
            .validBlock(CNBlocks.TURBINE_CONTROLLER)
            .register();

    public static final BlockEntityEntry<TurbineSteamInputValveBlockEntity> TURBINE_STEAM_INPUT_VALVE = REGISTRATE
            .blockEntity("turbine_steam_input_valve", TurbineSteamInputValveBlockEntity::new)
            .validBlock(CNBlocks.TURBINE_STEAM_INPUT_VALVE)
            .register();

    public static final BlockEntityEntry<TurbineGeneratorBlockEntity> TURBINE_GENERATOR = REGISTRATE
            .blockEntity("turbine_generator", TurbineGeneratorBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT))
            .validBlock(CNBlocks.TURBINE_GENERATOR)
            .renderer(() -> ShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<TurbineRotorBlockEntity> TURBINE_ROTOR = REGISTRATE
            .blockEntity("turbine_rotor", TurbineRotorBlockEntity::new)
            .visual(() -> TurbineRotorVisual::new)
            .validBlocks(CNBlocks.TURBINE_ROTOR, CNBlocks.LARGE_TURBINE_ROTOR)
            .renderer(() -> TurbineRotorRenderer::new)
            .register();

    public static final BlockEntityEntry<PrecisionUnmixerBlockEntity> PRECISION_UNMIXER = REGISTRATE
            .blockEntity("precision_unmixer", PrecisionUnmixerBlockEntity::new)
            .validBlock(CNBlocks.PRECISION_UNMIXER)
            .renderer(() -> PrecisionUnmixerRenderer::new)
            .register();

    public static final BlockEntityEntry<IndustrialHeaterBlockEntity> INDUSTRIAL_HEATER = REGISTRATE
            .blockEntity("industrial_heater", IndustrialHeaterBlockEntity::new)
            .validBlock(CNBlocks.INDUSTRIAL_HEATER)
            .register();

    public static final BlockEntityEntry<EvaporationBasinBlockEntity> EVAPORATION_BASIN = REGISTRATE
            .blockEntity("evaporation_basin", EvaporationBasinBlockEntity::new)
            .validBlock(CNBlocks.EVAPORATION_BASIN)
            .renderer(() -> EvaporationBasinRenderer::new)
            .register();

    public static final BlockEntityEntry<ExhaustHoodBlockEntity> EXHAUST_HOOD = REGISTRATE
            .blockEntity("exhaust_hood", ExhaustHoodBlockEntity::new)
            .validBlock(CNBlocks.EXHAUST_HOOD)
            .register();

    public static final BlockEntityEntry<MeteringValveBlockEntity> METERING_VALVE = REGISTRATE
            .blockEntity("metering_valve", MeteringValveBlockEntity::new)
            .validBlock(CNBlocks.METERING_VALVE)
            .renderer(() -> MeteringValveRenderer::new)
            .register();

    public static final BlockEntityEntry<SpentSaltBlockEntity> SPENT_SALT = REGISTRATE
            .blockEntity("spent_salt", SpentSaltBlockEntity::new)
            .validBlock(spentSaltBlock(CNFluids.SPENT_URANIUM_SALT))
            .validBlock(spentSaltBlock(CNFluids.SPENT_PLUTONIUM_SALT))
            .validBlock(spentSaltBlock(CNFluids.SPENT_THORIUM_URANIUM_SALT))
            .register();

    private static NonNullSupplier<Block> spentSaltBlock(FluidEntry<?> fluid) {
        return () -> fluid.getSource().defaultFluidState().createLegacyBlock().getBlock();
    }

    private CNBlockEntities() {}

    public static void register() {}
}
