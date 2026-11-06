package com.mocretion.createnuclearized.content.advancement;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNCriteriaTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CreateNuclearized.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, ReactorStartedTrigger> REACTOR_STARTED =
            TRIGGERS.register("reactor_started", ReactorStartedTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, TurbineStartedTrigger> TURBINE_STARTED =
            TRIGGERS.register("turbine_started", TurbineStartedTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, GasExposureTrigger> GAS_EXPOSURE =
            TRIGGERS.register("gas_exposure", GasExposureTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, SpentSaltProducedTrigger> SPENT_SALT_PRODUCED =
            TRIGGERS.register("spent_salt_produced", SpentSaltProducedTrigger::new);

    private CNCriteriaTriggers() {}
}
