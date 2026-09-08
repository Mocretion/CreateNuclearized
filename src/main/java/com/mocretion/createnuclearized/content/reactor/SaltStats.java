package com.mocretion.createnuclearized.content.reactor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SaltStats(int heat, int uranium, int plutonium, int thorium, int byproducts) {

    // Pure salt, no reaction
    public static final SaltStats EMPTY = new SaltStats(0, 0, 0, 0, 0);

    // Rough uranium at base stats default, kept as a fallback
    // ReactorFuel holds each fuels real baseline and ReactorStats derives the actual output from the layout
    public static final SaltStats BASE = new SaltStats(200, 50, 20, 0, 30);

    public static final Codec<SaltStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("heat").forGetter(SaltStats::heat),
            Codec.INT.fieldOf("uranium").forGetter(SaltStats::uranium),
            Codec.INT.fieldOf("plutonium").forGetter(SaltStats::plutonium),
            Codec.INT.fieldOf("thorium").forGetter(SaltStats::thorium),
            Codec.INT.fieldOf("byproducts").forGetter(SaltStats::byproducts)
    ).apply(i, SaltStats::new));

    public static final StreamCodec<ByteBuf, SaltStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaltStats::heat,
            ByteBufCodecs.VAR_INT, SaltStats::uranium,
            ByteBufCodecs.VAR_INT, SaltStats::plutonium,
            ByteBufCodecs.VAR_INT, SaltStats::thorium,
            ByteBufCodecs.VAR_INT, SaltStats::byproducts,
            SaltStats::new);
}
