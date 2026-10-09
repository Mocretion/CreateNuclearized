package com.mocretion.createnuclearized.content.reactor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SaltStats(int heat, int uranium, int plutonium, int thorium, int americium, int offGas) {

    public static final SaltStats EMPTY = new SaltStats(0, 0, 0, 0, 0, 0);

    public static final Codec<SaltStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("heat").forGetter(SaltStats::heat),
            Codec.INT.fieldOf("uranium").forGetter(SaltStats::uranium),
            Codec.INT.fieldOf("plutonium").forGetter(SaltStats::plutonium),
            Codec.INT.fieldOf("thorium").forGetter(SaltStats::thorium),
            Codec.INT.fieldOf("americium").forGetter(SaltStats::americium),
            Codec.INT.fieldOf("off_gas").forGetter(SaltStats::offGas)
    ).apply(i, SaltStats::new));

    public static final StreamCodec<ByteBuf, SaltStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaltStats::heat,
            ByteBufCodecs.VAR_INT, SaltStats::uranium,
            ByteBufCodecs.VAR_INT, SaltStats::plutonium,
            ByteBufCodecs.VAR_INT, SaltStats::thorium,
            ByteBufCodecs.VAR_INT, SaltStats::americium,
            ByteBufCodecs.VAR_INT, SaltStats::offGas,
            SaltStats::new);

    public int amount(ReactorFuel.Element element) {
        return switch (element) {
            case URANIUM -> uranium;
            case PLUTONIUM -> plutonium;
            case THORIUM -> thorium;
            case AMERICIUM -> americium;
        };
    }

    public SaltStats withElement(ReactorFuel.Element element, int value) {
        return new SaltStats(heat,
                element == ReactorFuel.Element.URANIUM ? value : uranium,
                element == ReactorFuel.Element.PLUTONIUM ? value : plutonium,
                element == ReactorFuel.Element.THORIUM ? value : thorium,
                element == ReactorFuel.Element.AMERICIUM ? value : americium,
                offGas);
    }

    public boolean isEmpty() {
        return heat == 0 && uranium == 0 && plutonium == 0 && thorium == 0 && americium == 0 && offGas == 0;
    }
}
