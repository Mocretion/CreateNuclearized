package com.mocretion.createnuclearized.registry;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CNParticleTypes {

    public static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateNuclearized.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> GAS_CLOUD =
            TYPES.register("gas_cloud", CNParticleTypes::colored);

    private CNParticleTypes() {}

    private static ParticleType<ColorParticleOption> colored() {
        return new ParticleType<>(false) {
            private final MapCodec<ColorParticleOption> codec = ColorParticleOption.codec(this);
            private final StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec =
                    ColorParticleOption.streamCodec(this);

            @Override
            public MapCodec<ColorParticleOption> codec() {
                return codec;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                return streamCodec;
            }
        };
    }
}
