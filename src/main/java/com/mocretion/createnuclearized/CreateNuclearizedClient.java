package com.mocretion.createnuclearized;

import com.mocretion.createnuclearized.content.boiler.ContainedBlazeBurnerPartials;
import com.mocretion.createnuclearized.content.controlrod.ControlRodPartials;
import com.mocretion.createnuclearized.content.turbine.TurbineRotorPartials;
import com.mocretion.createnuclearized.content.unmixer.UnmixerPartials;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerScreen;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorScreen;
import com.mocretion.createnuclearized.content.turbine.TurbineControllerScreen;
import com.mocretion.createnuclearized.registry.CNMenuTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateNuclearized.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateNuclearized.MODID, value = Dist.CLIENT)
public class CreateNuclearizedClient {
    public CreateNuclearizedClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ControlRodPartials.init();
        ContainedBlazeBurnerPartials.init();
        TurbineRotorPartials.init();
        UnmixerPartials.init();
        CreateNuclearized.LOGGER.info("Create: Nuclearized client setup");
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CNMenuTypes.REACTOR_CONTROLLER.get(), ReactorControllerScreen::new);
        event.register(CNMenuTypes.REACTOR_MONITOR.get(), ReactorMonitorScreen::new);
        event.register(CNMenuTypes.TURBINE_CONTROLLER.get(), TurbineControllerScreen::new);
    }
}
