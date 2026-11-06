package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.gas.GasFanProcessingType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNFanProcessingTypes {

    public static final DeferredRegister<FanProcessingType> TYPES =
            DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, CreateNuclearized.MODID);

    public static final DeferredHolder<FanProcessingType, GasFanProcessingType> GAS =
            TYPES.register("gas", GasFanProcessingType::new);

    private CNFanProcessingTypes() {}
}
