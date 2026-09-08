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

    /** Append the "Heated Salt Composition" header and a line per non trivial stat */
    public static void append(List<Component> tooltip, SaltStats stats, @Nullable ReactorFuel fuel) {
        lang("gui.goggles.salt_stats").style(ChatFormatting.WHITE).forGoggles(tooltip);
        statLine(tooltip, "gui.goggles.salt_heat", stats.heat(), "°", null);
        statLineIfNonzero(tooltip, "gui.goggles.salt_uranium", stats.uranium(), "%",
                role(fuel, ReactorFuel.Element.URANIUM));
        statLineIfNonzero(tooltip, "gui.goggles.salt_plutonium", stats.plutonium(), "%",
                role(fuel, ReactorFuel.Element.PLUTONIUM));
        statLineIfNonzero(tooltip, "gui.goggles.salt_thorium", stats.thorium(), "%",
                role(fuel, ReactorFuel.Element.THORIUM));
        statLineIfNonzero(tooltip, "gui.goggles.salt_byproducts", stats.byproducts(), "%", null);
    }

    /**
     * Append an output valves own per channel knobs (burn and purity, what the player tunes with moderators/coolers), the heat that burn
     * makes, and its rate. Purity is skipped for a fuel that generates nothing, where it does nothing
     */
    public static void appendChannel(List<Component> tooltip, int burn, int purity, double rate,
            @Nullable ReactorFuel fuel) {
        lang("gui.goggles.salt_channel").style(ChatFormatting.WHITE).forGoggles(tooltip);
        statLine(tooltip, "gui.goggles.salt_burn", burn, "%", null);
        if (fuel == null || fuel.generated() != null)
            statLine(tooltip, "gui.goggles.salt_purity", purity, "%", null);
        if (fuel != null)
            statLine(tooltip, "gui.goggles.salt_heat", fuel.heatFor(burn), "°", null);
        lang("gui.goggles.salt_rate")
                .style(ChatFormatting.GRAY)
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.text(formatRate(rate)).style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);
    }

    @Nullable
    private static String role(@Nullable ReactorFuel fuel, ReactorFuel.Element element) {
        return fuel == null ? null : fuel.role(element);
    }

    /** Trim a rate to at most 4 decimals with no trailing zeros, e.g. 0.05 or 0.0075 */
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
