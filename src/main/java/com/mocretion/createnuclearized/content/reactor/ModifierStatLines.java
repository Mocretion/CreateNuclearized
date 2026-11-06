package com.mocretion.createnuclearized.content.reactor;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ModifierStatLines {

    private static final String INDENT = "  ";

    private ModifierStatLines() {}

    public static void appendStats(List<Component> lines, int dBurn, int dPurity, double dRate, int dHeat) {
        appendStats(lines, dBurn, dPurity, dRate, dHeat, "", false);
    }

    public static void appendIndentedStats(List<Component> lines, int dBurn, int dPurity, double dRate, int dHeat) {
        appendStats(lines, dBurn, dPurity, dRate, dHeat, INDENT, true);
    }

    private static void appendStats(List<Component> lines, int dBurn, int dPurity, double dRate, int dHeat,
            String prefix, boolean icons) {
        if (dBurn != 0) lines.add(stat(prefix, "burn", signedInt(dBurn), icons ? SaltIcon.BURN : null));
        if (dPurity != 0) lines.add(stat(prefix, "purity", signedInt(dPurity), icons ? SaltIcon.PURITY : null));
        if (dHeat != 0) lines.add(stat(prefix, "heat", signedInt(dHeat), icons ? SaltIcon.HEAT : null));
        if (dRate != 0) lines.add(stat(prefix, "rate", RateFormat.signed(dRate), icons ? SaltIcon.RATE : null));
    }

    private static MutableComponent stat(String prefix, String key, String value, @Nullable SaltIcon icon) {
        MutableComponent body = Component.translatable("createnuclearized.tooltip.stat." + key, value)
                .withStyle(ChatFormatting.GRAY);
        MutableComponent line = icon == null ? body : icon.prefix(body);
        return prefix.isEmpty() ? line : Component.literal(prefix).append(line);
    }

    private static String signedInt(int value) {
        return (value > 0 ? "+" : "") + value;
    }
}
