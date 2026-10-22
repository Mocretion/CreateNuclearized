package com.mocretion.createnuclearized.content.reactor;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record SaltStats(int heat, Map<ResourceLocation, Integer> elements, int offGas) {

    public static final SaltStats EMPTY = new SaltStats(0, Map.of(), 0);

    public static final Codec<SaltStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("heat").forGetter(SaltStats::heat),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("elements", Map.of()).forGetter(SaltStats::elements),
            Codec.INT.fieldOf("off_gas").forGetter(SaltStats::offGas)
    ).apply(i, SaltStats::new));

    public static final StreamCodec<ByteBuf, SaltStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaltStats::heat,
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), SaltStats::elements,
            ByteBufCodecs.VAR_INT, SaltStats::offGas,
            SaltStats::new);

    public SaltStats {
        Map<ResourceLocation, Integer> kept = new HashMap<>();
        elements.forEach((id, amount) -> {
            if (amount != 0) kept.put(id, amount);
        });
        elements = Map.copyOf(kept);
    }

    public int amount(SaltElement element) {
        return elements.getOrDefault(element.id(), 0);
    }

    public SaltStats withElement(SaltElement element, int value) {
        Map<ResourceLocation, Integer> next = new HashMap<>(elements);
        next.put(element.id(), value);
        return new SaltStats(heat, next, offGas);
    }

    public SaltStats withHeat(int value) {
        return new SaltStats(value, elements, offGas);
    }

    public SaltStats withOffGas(int value) {
        return new SaltStats(heat, elements, value);
    }

    public boolean isEmpty() {
        return heat == 0 && elements.isEmpty() && offGas == 0;
    }
}
