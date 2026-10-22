package com.mocretion.createnuclearized.content.metal;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record MetalDefinition(ResourceLocation id, String name, int color, Optional<Integer> radiationSetting,
        Optional<Boolean> enriched, Optional<Integer> enrichedColorSetting, Salt salt) {

    public static final int MAX_RADIATION = 255;
    public static final int DEFAULT_RADIATION = 1;
    public static final int DEFAULT_MAX_HEAT = 4000;
    public static final int DEFAULT_BURN = 50;
    public static final int DEFAULT_PURITY = 40;
    public static final String NO_ELEMENT = "none";
    private static final float SALT_LIGHTEN = 0.8f;
    private static final float SPENT_LIGHTEN = 0.6f;
    private static final float ENRICHED_LIGHTEN = 0.15f;

    private static final Codec<Integer> COLOR = Codec.STRING.comapFlatMap(MetalDefinition::parseColor,
            c -> String.format("#%06X", c & 0xFFFFFF));

    public record Salt(Optional<Boolean> enabled, Optional<Integer> color, Optional<Integer> spentColor, Optional<Integer> maxHeat, Optional<Integer> burn,
            Optional<Integer> purity, Optional<String> unconsumed, Optional<String> bred) {

        public static final Salt DEFAULT = new Salt(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Salt> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("enabled").forGetter(Salt::enabled),
                COLOR.optionalFieldOf("color").forGetter(Salt::color),
                COLOR.optionalFieldOf("spent_color").forGetter(Salt::spentColor),
                Codec.intRange(1, 100000).optionalFieldOf("max_heat").forGetter(Salt::maxHeat),
                Codec.intRange(0, 100).optionalFieldOf("burn").forGetter(Salt::burn),
                Codec.intRange(0, 100).optionalFieldOf("purity").forGetter(Salt::purity),
                Codec.STRING.optionalFieldOf("unconsumed").forGetter(Salt::unconsumed),
                Codec.STRING.optionalFieldOf("bred").forGetter(Salt::bred)
        ).apply(i, Salt::new));

        public int maxHeatOrDefault() {
            return maxHeat.orElse(DEFAULT_MAX_HEAT);
        }

        public int burnOrDefault() {
            return burn.orElse(DEFAULT_BURN);
        }

        public int purityOrDefault() {
            return purity.orElse(DEFAULT_PURITY);
        }
    }

    public static final Codec<Body> BODY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Body::name),
            COLOR.fieldOf("color").forGetter(Body::color),
            Codec.intRange(0, MAX_RADIATION).optionalFieldOf("radiation").forGetter(Body::radiation),
            Codec.BOOL.optionalFieldOf("enriched").forGetter(Body::enriched),
            COLOR.optionalFieldOf("enriched_color").forGetter(Body::enrichedColor),
            Salt.CODEC.optionalFieldOf("salt", Salt.DEFAULT).forGetter(Body::salt)
    ).apply(i, Body::new));

    public record Body(String name, int color, Optional<Integer> radiation, Optional<Boolean> enriched, Optional<Integer> enrichedColor, Salt salt) {
        public MetalDefinition withId(ResourceLocation id) {
            return new MetalDefinition(id, name, color, radiation, enriched, enrichedColor, salt);
        }
    }

    public int radiation() {
        return radiationSetting.orElse(DEFAULT_RADIATION);
    }

    public boolean hasSalt() {
        return salt.enabled().orElse(true);
    }

    public String slug() {
        return id.getPath();
    }

    public ResourceLocation elementId() {
        return id;
    }

    public ResourceLocation liquidId() {
        return derived("liquid_" + slug());
    }

    public ResourceLocation enrichedLiquidId() {
        return derived("enriched_liquid_" + slug());
    }

    public int enrichedColor() {
        return enrichedColorSetting.orElseGet(() -> lighten(color, ENRICHED_LIGHTEN));
    }

    public ResourceLocation saltId() {
        return derived(slug() + "_salt");
    }

    public ResourceLocation spentSaltId() {
        return derived("spent_" + slug() + "_salt");
    }

    public int saltColor() {
        return salt.color().orElseGet(() -> lighten(color, SALT_LIGHTEN));
    }

    public int spentSaltColor() {
        return salt.spentColor().orElseGet(() -> lighten(color, SPENT_LIGHTEN));
    }

    private ResourceLocation derived(String path) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }

    private static int lighten(int argb, float amount) {
        int r = mix((argb >> 16) & 0xFF, amount);
        int g = mix((argb >> 8) & 0xFF, amount);
        int b = mix(argb & 0xFF, amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int mix(int channel, float amount) {
        return Math.round(channel + (255 - channel) * amount);
    }

    private static DataResult<Integer> parseColor(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6 && hex.length() != 8)
            return DataResult.error(() -> "Color must be #RRGGBB or #AARRGGBB, got " + text);
        try {
            return DataResult.success(0xFF000000 | (int) Long.parseLong(hex.length() == 8 ? hex.substring(2) : hex, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid hex color " + text);
        }
    }
}
