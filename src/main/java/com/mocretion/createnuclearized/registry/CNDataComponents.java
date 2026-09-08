package com.mocretion.createnuclearized.registry;

import java.util.function.Supplier;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.reactor.SaltStats;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNDataComponents {

    public static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateNuclearized.MODID);

    // Stats carried by heated salt (see SaltStats)
    // Persisted to NBT so it survives in valve tanks and the save, and network synced so the client can render it (goggle overlay)
    public static final Supplier<DataComponentType<SaltStats>> SALT_STATS =
            TYPES.register("salt_stats", () -> DataComponentType.<SaltStats>builder()
                    .persistent(SaltStats.CODEC)
                    .networkSynchronized(SaltStats.STREAM_CODEC)
                    .build());

    private CNDataComponents() {}

    /** No op that forces this class to load so the field above initializes before the event fires */
    public static void register() {}
}
