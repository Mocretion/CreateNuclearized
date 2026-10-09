package com.mocretion.createnuclearized.content.reactor;

import java.util.List;
import java.util.Locale;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CreateNuclearized.MODID, value = Dist.CLIENT)
public final class ReactorTooltips {

    private ReactorTooltips() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();

        if (stack.getItem() == CNBlocks.PRECISION_UNMIXER.get().asItem()) {
            lines.add(Component.translatable("createnuclearized.tooltip.precision_unmixer.flavor")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }

        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null) {
            SaltStatsTooltip.append(lines, stats, spentBucketFuel(stack));
            return;
        }

        for (ReactorFuel fuel : ReactorFuel.values()) {
            if (stack.getItem() == fuel.inputFluid().getBucket()) {
                appendFuelBase(lines, fuel);
                return;
            }
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            double moderatorRate = ReactorModerator.rate(state);
            if (moderatorRate > 0) {
                appendModerator(lines, moderatorRate);
                return;
            }
            ReactorModifier.Effect effect = ReactorModifier.effect(state);
            if (effect != null) {
                appendModifier(lines, effect);
                return;
            }
        }
        if (stack.is(Items.WATER_BUCKET))
            appendModifier(lines, ReactorModifier.WATER_SOURCE);
    }

    private static ReactorFuel spentBucketFuel(ItemStack stack) {
        for (ReactorFuel fuel : ReactorFuel.values()) {
            if (stack.getItem() == fuel.outputFluid().getBucket()) return fuel;
        }
        return null;
    }

    private static void appendModerator(List<Component> lines, double rate) {
        lines.add(Component.translatable("createnuclearized.tooltip.moderator")
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("createnuclearized.tooltip.moderator.rate", signed(rate))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("createnuclearized.tooltip.moderator.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendModifier(List<Component> lines, ReactorModifier.Effect effect) {
        lines.add(Component.translatable("createnuclearized.tooltip.modifier")
                .withStyle(ChatFormatting.GOLD));
        addDeltaLine(lines, "burn", signedInt(effect.dBurn()), effect.dBurn() != 0);
        addDeltaLine(lines, "purity", signedInt(effect.dPurity()), effect.dPurity() != 0);
        if (effect.dRate() != 0) {
            lines.add(Component.translatable("createnuclearized.tooltip.rate", signed(effect.dRate()))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("createnuclearized.tooltip.modifier.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        appendBurnPurityHelp(lines);
    }

    private static void appendFuelBase(List<Component> lines, ReactorFuel fuel) {
        lines.add(Component.translatable("createnuclearized.tooltip.fuel")
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("createnuclearized.tooltip.fuel.heat", fuel.heatFor(fuel.baseBurn()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("createnuclearized.tooltip.fuel.burn", fuel.baseBurn())
                .withStyle(ChatFormatting.GRAY));
        if (fuel.bred() != null) {
            lines.add(Component.translatable("createnuclearized.tooltip.fuel.purity", fuel.basePurity())
                    .withStyle(ChatFormatting.GRAY));
        }
        addRoleElement(lines, "unconsumed", fuel.unconsumed());
        addRoleElement(lines, "bred", fuel.bred());
        appendBurnPurityHelp(lines);
    }

    private static void appendBurnPurityHelp(List<Component> lines) {
        if (Screen.hasShiftDown()) {
            lines.add(Component.translatable("createnuclearized.tooltip.help.burn")
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("createnuclearized.tooltip.help.burn.desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("createnuclearized.tooltip.help.purity")
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("createnuclearized.tooltip.help.purity.desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable("createnuclearized.tooltip.help.hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static void addRoleElement(List<Component> lines, String roleKey, ReactorFuel.Element element) {
        if (element == null) return;
        lines.add(Component.translatable("createnuclearized.tooltip.fuel." + roleKey,
                        Component.translatable("createnuclearized.tooltip.element." + element.name().toLowerCase(Locale.ROOT)))
                .withStyle(ChatFormatting.GRAY));
    }

    private static void addDeltaLine(List<Component> lines, String stat, String value, boolean show) {
        if (!show) return;
        lines.add(Component.translatable("createnuclearized.tooltip.delta." + stat, value)
                .withStyle(ChatFormatting.GRAY));
    }

    private static String signedInt(int value) {
        return (value > 0 ? "+" : "") + value;
    }

    private static String signed(double value) {
        String s = String.format(Locale.ROOT, "%.4f", value);
        if (s.contains("."))
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return (value > 0 ? "+" : "") + s;
    }
}
