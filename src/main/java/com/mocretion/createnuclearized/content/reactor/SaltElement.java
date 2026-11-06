package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.fluid.TexturedFluidType;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

public final class SaltElement {

    private static final Map<ResourceLocation, SaltElement> BY_ID = new LinkedHashMap<>();
    private static final int RGB_MASK = 0xFFFFFF;

    public static final SaltElement URANIUM = register(builtinId("uranium"), CNFluids::liquidUranium);
    public static final SaltElement PLUTONIUM = register(builtinId("plutonium"), CNFluids::liquidPlutonium);
    public static final SaltElement THORIUM = register(builtinId("thorium"), CNFluids::liquidThorium);
    public static final SaltElement AMERICIUM = register(builtinId("americium"), CNFluids::liquidAmericium);

    public static final Codec<SaltElement> CODEC = Codec.STRING.comapFlatMap(SaltElement::parse, SaltElement::idString);

    private final ResourceLocation id;
    private final Supplier<Fluid> liquid;

    private SaltElement(ResourceLocation id, Supplier<Fluid> liquid) {
        this.id = id;
        this.liquid = liquid;
    }

    public static synchronized SaltElement register(ResourceLocation id, Supplier<Fluid> liquid) {
        SaltElement element = new SaltElement(id, liquid);
        if (BY_ID.putIfAbsent(id, element) != null)
            throw new IllegalStateException("Duplicate salt element " + id);
        return element;
    }

    @Nullable
    public static SaltElement byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Collection<SaltElement> values() {
        return Collections.unmodifiableCollection(new ArrayList<>(BY_ID.values()));
    }

    public ResourceLocation id() {
        return id;
    }

    public Fluid liquid() {
        return liquid.get();
    }

    public int color() {
        return liquid().getFluidType() instanceof TexturedFluidType textured ? textured.getTint() & RGB_MASK : RGB_MASK;
    }

    public Component displayName() {
        return Component.translatable("element." + id.getNamespace() + "." + id.getPath());
    }

    private String idString() {
        return id.toString();
    }

    private static ResourceLocation builtinId(String name) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, name);
    }

    private static DataResult<SaltElement> parse(String text) {
        ResourceLocation id = text.contains(":")
                ? ResourceLocation.tryParse(text)
                : ResourceLocation.tryBuild(CreateNuclearized.MODID, text);
        if (id == null) return DataResult.error(() -> "Invalid salt element id " + text);
        SaltElement element = BY_ID.get(id);
        return element == null ? DataResult.error(() -> "Unknown salt element " + id) : DataResult.success(element);
    }
}
