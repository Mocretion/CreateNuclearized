package com.mocretion.createnuclearized;

import com.mocretion.createnuclearized.content.boiler.IndustrialHeaterPartials;
import com.mocretion.createnuclearized.content.controlrod.ControlRodPartials;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorPartials;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorRenderer;
import com.mocretion.createnuclearized.content.unmixer.UnmixerPartials;
import com.mocretion.createnuclearized.content.multiblock.ShellFacingValveModel;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerScreen;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorScreen;
import com.mocretion.createnuclearized.content.reactor.ReactorMultiblock;
import com.mocretion.createnuclearized.content.turbine.TurbineMultiblock;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.content.turbine.TurbineControllerScreen;
import com.mocretion.createnuclearized.content.metal.Metals;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mocretion.createnuclearized.registry.CNMenuTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@Mod(value = CreateNuclearized.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateNuclearized.MODID, value = Dist.CLIENT)
public class CreateNuclearizedClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ControlRodPartials.init();
        IndustrialHeaterPartials.init();
        TurbineRotorPartials.init();
        TurbineRotorRenderer.registerCompartments();
        UnmixerPartials.init();
        CreateNuclearized.LOGGER.info("Create: Nuclearized client setup");
    }

    private static final int MONAZITE_SAND_TINT = 0x9C8E5A;

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> buckets = new ArrayList<>(CNFluids.tintedBuckets());
        buckets.addAll(Metals.allBuckets());
        event.register(new DynamicFluidContainerModel.Colors(), buckets.toArray(Item[]::new));
        event.register((stack, tintIndex) -> tintIndex == 0 ? MONAZITE_SAND_TINT : -1, CNBlocks.MONAZITE_SAND.get());
    }

    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? MONAZITE_SAND_TINT : -1, CNBlocks.MONAZITE_SAND.get());
    }

    private static final ResourceLocation REACTOR_VALVE_CASING_MODEL =
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/valve_casing");
    private static final ResourceLocation TURBINE_VALVE_CASING_MODEL =
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/turbine_valve_casing");

    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(REACTOR_VALVE_CASING_MODEL));
        event.register(ModelResourceLocation.standalone(TURBINE_VALVE_CASING_MODEL));
    }

    @SubscribeEvent
    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        wrapShellFacingValve(event, CNBlocks.FISSILE_SALT_INPUT_VALVE.get(), REACTOR_VALVE_CASING_MODEL,
                ReactorMultiblock::isShellMember);
        wrapShellFacingValve(event, CNBlocks.TURBINE_STEAM_INPUT_VALVE.get(), TURBINE_VALVE_CASING_MODEL,
                TurbineMultiblock::isShellMember);
    }

    private static void wrapShellFacingValve(ModelEvent.ModifyBakingResult event, Block valve,
            ResourceLocation casingModel, Predicate<BlockState> isShell) {
        BakedModel casing = event.getModels().get(ModelResourceLocation.standalone(casingModel));
        if (casing == null) return;
        for (BlockState state : valve.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = event.getModels().get(key);
            if (original != null)
                event.getModels().put(key, new ShellFacingValveModel(original, casing, isShell));
        }
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CNMenuTypes.REACTOR_CONTROLLER.get(), ReactorControllerScreen::new);
        event.register(CNMenuTypes.REACTOR_MONITOR.get(), ReactorMonitorScreen::new);
        event.register(CNMenuTypes.TURBINE_CONTROLLER.get(), TurbineControllerScreen::new);
    }
}
