package com.mocretion.createnuclearized.content.metal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltElement;

import net.minecraft.resources.ResourceLocation;

public final class MetalOverrides {

    private static final Set<String> BUILTIN = Set.of("uranium", "plutonium", "thorium", "americium", "thorium_uranium");
    private static final Map<String, MetalDefinition> BY_SLUG = new HashMap<>();

    private MetalOverrides() {}

    static boolean isBuiltin(ResourceLocation id) {
        return id.getNamespace().equals(CreateNuclearized.MODID) && BUILTIN.contains(id.getPath());
    }

    static void add(MetalDefinition definition) {
        BY_SLUG.put(definition.slug(), definition);
        CreateNuclearized.LOGGER.info("Overriding built in metal {}", definition.id());
    }

    public static int liquidColor(@Nullable String slug, int fallback) {
        MetalDefinition definition = definition(slug);
        return definition == null ? fallback : definition.color();
    }

    public static int enrichedColor(@Nullable String slug, int fallback) {
        MetalDefinition definition = definition(slug);
        return definition == null ? fallback : definition.enrichedColorSetting().orElse(fallback);
    }

    public static int saltColor(@Nullable String slug, int fallback) {
        MetalDefinition definition = definition(slug);
        return definition == null ? fallback : definition.salt().color().orElse(fallback);
    }

    public static int spentColor(@Nullable String slug, int fallback) {
        MetalDefinition definition = definition(slug);
        return definition == null ? fallback : definition.salt().spentColor().orElse(fallback);
    }

    public static int radiation(@Nullable String slug, int fallback) {
        MetalDefinition definition = definition(slug);
        return definition == null ? fallback : definition.radiationSetting().orElse(fallback);
    }

    public static int spentTemperature(@Nullable String slug, int fallback) {
        ReactorFuel fuel = fuel(slug);
        return fuel == null ? fallback : fuel.heatFor(fuel.baseBurn());
    }

    static void applyFuelStats() {
        for (MetalDefinition definition : BY_SLUG.values()) {
            ReactorFuel fuel = fuel(definition.slug());
            MetalDefinition.Salt salt = definition.salt();
            if (fuel == null) continue;
            if (!definition.hasSalt()) {
                fuel.setDisabled(true);
                continue;
            }
            salt.maxHeat().ifPresent(fuel::setMaxHeat);
            salt.burn().ifPresent(fuel::setBaseBurn);
            salt.purity().ifPresent(fuel::setBasePurity);
            salt.unconsumed().ifPresent(id -> fuel.setUnconsumed(element(id)));
            salt.bred().ifPresent(id -> fuel.setBred(element(id)));
        }
    }

    @Nullable
    private static SaltElement element(String text) {
        return MetalDefinition.NO_ELEMENT.equals(text) ? null : Metals.lookup(text);
    }

    @Nullable
    private static MetalDefinition definition(@Nullable String slug) {
        return slug == null ? null : BY_SLUG.get(slug);
    }

    @Nullable
    private static ReactorFuel fuel(@Nullable String slug) {
        if (slug == null) return null;
        return switch (slug) {
            case "uranium" -> ReactorFuel.URANIUM;
            case "plutonium" -> ReactorFuel.PLUTONIUM;
            case "thorium_uranium" -> ReactorFuel.THORIUM_URANIUM;
            default -> null;
        };
    }
}
