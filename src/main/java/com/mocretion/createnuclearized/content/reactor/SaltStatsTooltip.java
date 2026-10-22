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
            if (fuel.bred() != fuel.unconsumed())
                elementLine(tooltip, stats, fuel.bred(), fuel);
        } else {
            for (SaltElement element : SaltElement.values())
                statLineIfNonzero(tooltip, element.displayName(), stats.amount(element), "%", null);
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
                .add(CreateLang.text(RateFormat.format(rate) + " mB/t").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);
    }

    private static void elementLine(List<Component> tooltip, SaltStats stats, @Nullable SaltElement element,
            ReactorFuel fuel) {
        if (element == null) return;
        statLineIfNonzero(tooltip, element.displayName(), stats.amount(element), "%", fuel.role(element));
    }

    private static LangBuilder lang(String key) {
        return new LangBuilder(CreateNuclearized.MODID).translate(key);
    }

    private static void statLineIfNonzero(List<Component> tooltip, String key, int value, String unit,
            @Nullable String roleKey) {
        if (value != 0)
            statLine(tooltip, key, value, unit, roleKey);
    }

    private static void statLineIfNonzero(List<Component> tooltip, Component label, int value, String unit,
            @Nullable String roleKey) {
        if (value != 0)
            statLine(tooltip, new LangBuilder(CreateNuclearized.MODID).add(label), value, unit, roleKey);
    }

    private static void statLine(List<Component> tooltip, String key, int value, String unit,
            @Nullable String roleKey) {
        statLine(tooltip, lang(key), value, unit, roleKey);
    }

    private static void statLine(List<Component> tooltip, LangBuilder label, int value, String unit,
            @Nullable String roleKey) {
        LangBuilder line = label
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
