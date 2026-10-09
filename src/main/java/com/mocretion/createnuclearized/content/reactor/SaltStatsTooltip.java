package com.mocretion.createnuclearized.content.reactor;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class SaltStatsTooltip {

    private SaltStatsTooltip() {}

    public static void append(List<Component> tooltip, SaltStats stats, @Nullable ReactorFuel fuel) {
        append(tooltip, stats, fuel, "gui.goggles.salt_stats");
    }

    public static void append(List<Component> tooltip, SaltStats stats, @Nullable ReactorFuel fuel, String headerKey) {
        lang(headerKey).style(ChatFormatting.WHITE).forGoggles(tooltip);
        statLine(tooltip, "gui.goggles.salt_heat", stats.heat(), "°", null);
        if (fuel != null) {
            elementLine(tooltip, stats, fuel.unconsumed(), fuel);
            elementLine(tooltip, stats, fuel.bred(), fuel);
        } else {
            for (ReactorFuel.Element element : ReactorFuel.Element.values())
                statLineIfNonzero(tooltip, elementKey(element), stats.amount(element), "%", null);
        }
        statLineIfNonzero(tooltip, "gui.goggles.salt_off_gas", stats.offGas(), "%", null);
    }

    public static void appendChannel(List<Component> tooltip, int burn, int purity, double rate,
            @Nullable ReactorFuel fuel) {
        lang("gui.goggles.salt_channel").style(ChatFormatting.WHITE).forGoggles(tooltip);
        statLine(tooltip, "gui.goggles.salt_burn", burn, "%", null);
        if (fuel == null || fuel.bred() != null)
            statLine(tooltip, "gui.goggles.salt_purity", purity, "%", null);
        if (fuel != null)
            statLine(tooltip, "gui.goggles.salt_heat", fuel.heatFor(burn), "°", null);
        lang("gui.goggles.salt_rate")
                .style(ChatFormatting.GRAY)
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.text(formatRate(rate)).style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);
    }

    private static void elementLine(List<Component> tooltip, SaltStats stats, @Nullable ReactorFuel.Element element,
            ReactorFuel fuel) {
        if (element == null) return;
        statLineIfNonzero(tooltip, elementKey(element), stats.amount(element), "%", fuel.role(element));
    }

    private static String elementKey(ReactorFuel.Element element) {
        return "gui.goggles.salt_" + element.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String formatRate(double rate) {
        String s = String.format(java.util.Locale.ROOT, "%.4f", rate);
        if (s.contains("."))
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s + " mB/t";
    }

    private static LangBuilder lang(String key) {
        return new LangBuilder(CreateNuclearized.MODID).translate(key);
    }

    private static void statLineIfNonzero(List<Component> tooltip, String key, int value, String unit,
            @Nullable String roleKey) {
        if (value != 0)
            statLine(tooltip, key, value, unit, roleKey);
    }

    private static void statLine(List<Component> tooltip, String key, int value, String unit,
            @Nullable String roleKey) {
        LangBuilder line = lang(key)
                .style(ChatFormatting.GRAY)
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number(value).style(ChatFormatting.GOLD).text(ChatFormatting.GOLD, unit));
        if (roleKey != null) {
            line.text(ChatFormatting.DARK_GRAY, " ")
                    .add(lang("tooltip.role." + roleKey).style(ChatFormatting.DARK_GRAY));
        }
        line.forGoggles(tooltip, 1);
    }
}
