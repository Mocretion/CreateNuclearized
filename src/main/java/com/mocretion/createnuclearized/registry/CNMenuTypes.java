package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.reactor.ReactorControllerMenu;
import com.mocretion.createnuclearized.content.reactor.ReactorMonitorMenu;
import com.mocretion.createnuclearized.content.turbine.TurbineControllerMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreateNuclearized.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControllerMenu>> REACTOR_CONTROLLER =
            MENUS.register("reactor_controller", () -> IMenuTypeExtension.create(
                    (containerId, inv, buf) -> new ReactorControllerMenu(containerId, inv, buf.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorMonitorMenu>> REACTOR_MONITOR =
            MENUS.register("reactor_monitor", () -> IMenuTypeExtension.create(
                    (containerId, inv, buf) -> new ReactorMonitorMenu(containerId, inv, buf.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<TurbineControllerMenu>> TURBINE_CONTROLLER =
            MENUS.register("turbine_controller", () -> IMenuTypeExtension.create(
                    (containerId, inv, buf) -> new TurbineControllerMenu(containerId, inv, buf.readBlockPos())));

    private CNMenuTypes() {}
}
