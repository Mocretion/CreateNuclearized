package com.mocretion.createnuclearized;

import com.mocretion.createnuclearized.content.controlrod.ControlRodPartials;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerScreen;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorScreen;
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
        // Wire up the built in config screen (Mods screen > this mod > Config)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Touch the partial model holder so Flywheel registers it before models bake
        ControlRodPartials.init();
        CreateNuclearized.LOGGER.info("Create: Nuclearized client setup");
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CNMenuTypes.REACTOR_CONTROLLER.get(), ReactorControllerScreen::new);
        event.register(CNMenuTypes.REACTOR_MONITOR.get(), ReactorMonitorScreen::new);
    }
}
