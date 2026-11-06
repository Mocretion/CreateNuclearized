package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

public enum SaltIcon {
    HEAT('\uE000'),
    UNCONSUMED('\uE001'),
    BRED('\uE002'),
    OFF_GAS('\uE003'),
    BURN('\uE004'),
    PURITY('\uE005'),
    RATE('\uE006');

    private static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "icons");
    private static final Style STYLE = Style.EMPTY.withFont(FONT).withColor(ChatFormatting.WHITE);

    private final char glyph;

    SaltIcon(char glyph) {
        this.glyph = glyph;
    }

    public MutableComponent component() {
        return Component.literal(String.valueOf(glyph)).withStyle(STYLE);
    }

    public MutableComponent tinted(int rgb) {
        return Component.literal(String.valueOf(glyph)).withStyle(STYLE.withColor(TextColor.fromRgb(rgb)));
    }

    public MutableComponent prefix(Component line) {
        return prefix(component(), line);
    }

    public static MutableComponent prefix(MutableComponent icon, Component line) {
        return Component.empty().append(icon).append(" ").append(line);
    }

    @Nullable
    public static SaltIcon forRole(@Nullable String roleKey) {
        if (roleKey == null) return null;
        return switch (roleKey) {
            case "unconsumed" -> UNCONSUMED;
            case "bred" -> BRED;
            default -> null;
        };
    }
}
