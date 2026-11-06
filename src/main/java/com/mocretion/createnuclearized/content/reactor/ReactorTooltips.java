package com.mocretion.createnuclearized.content.reactor;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
            SaltStatsTooltip.appendToItem(lines, stats, spentBucketFuel(stack));
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
            int moderatorHeat = ReactorModerator.heat(state);
            int reflectorHeat = ReactorReflector.heat(state);
            ReactorModifier.Effect effect = ReactorModifier.effect(state);
            if (moderatorRate > 0 || moderatorHeat != 0)
                appendModerator(lines, moderatorRate, moderatorHeat);
            if (reflectorHeat > 0)
                appendReflector(lines, reflectorHeat);
            if (effect != null)
                appendModifier(lines, effect);
        }
    }

    @Nullable
    private static ReactorFuel spentBucketFuel(ItemStack stack) {
        for (ReactorFuel fuel : ReactorFuel.values()) {
            if (stack.getItem() == fuel.outputFluid().getBucket()) return fuel;
        }
        return null;
    }

    private static void appendModerator(List<Component> lines, double rate, int heat) {
        lines.add(title("moderator", ChatFormatting.AQUA));
        if (!Screen.hasShiftDown()) {
            lines.add(shiftHint());
            return;
        }
        ModifierStatLines.appendStats(lines, 0, 0, rate, heat);
        lines.add(hint("moderator"));
    }

    private static void appendReflector(List<Component> lines, int heat) {
        lines.add(title("reflector", ChatFormatting.LIGHT_PURPLE));
        if (!Screen.hasShiftDown()) {
            lines.add(shiftHint());
            return;
        }
        ModifierStatLines.appendStats(lines, 0, 0, 0, heat);
        lines.add(hint("reflector"));
    }

    private static void appendModifier(List<Component> lines, ReactorModifier.Effect effect) {
        lines.add(title("modifier", ChatFormatting.GOLD));
        if (!Screen.hasShiftDown()) {
            lines.add(shiftHint());
            return;
        }
        ModifierStatLines.appendStats(lines, effect.dBurn(), effect.dPurity(), effect.dRate(), effect.dHeat());
        lines.add(hint("modifier"));
        appendBurnPurityHelp(lines);
    }

    private static void appendFuelBase(List<Component> lines, ReactorFuel fuel) {
        lines.add(Component.translatable("createnuclearized.tooltip.fuel")
                .withStyle(ChatFormatting.AQUA));
        lines.add(SaltIcon.HEAT.prefix(fuelStat("heat", fuel.maxHeat())));
        lines.add(SaltIcon.BURN.prefix(fuelStat("burn", fuel.baseBurn())));
        if (fuel.bred() != null)
            lines.add(SaltIcon.PURITY.prefix(fuelStat("purity", fuel.basePurity())));
        addRoleElement(lines, "unconsumed", fuel.unconsumed());
        addRoleElement(lines, "bred", fuel.bred());
        appendBurnPurityHelp(lines);
    }

    private static void appendBurnPurityHelp(List<Component> lines) {
        if (Screen.hasControlDown()) {
            lines.add(Component.translatable("createnuclearized.tooltip.help.burn")
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("createnuclearized.tooltip.help.burn.desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("createnuclearized.tooltip.help.purity")
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("createnuclearized.tooltip.help.purity.desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable("createnuclearized.tooltip.ctrl_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static MutableComponent title(String key, ChatFormatting color) {
        return Component.translatable("createnuclearized.tooltip." + key).withStyle(color);
    }

    private static MutableComponent hint(String key) {
        return Component.translatable("createnuclearized.tooltip." + key + ".hint")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent shiftHint() {
        return Component.translatable("createnuclearized.tooltip.shift_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    private static MutableComponent fuelStat(String key, int value) {
        return Component.translatable("createnuclearized.tooltip.fuel." + key, value).withStyle(ChatFormatting.GRAY);
    }

    private static void addRoleElement(List<Component> lines, String roleKey, SaltElement element) {
        if (element == null) return;
        MutableComponent line = Component.translatable("createnuclearized.tooltip.fuel." + roleKey, element.displayName())
                .withStyle(ChatFormatting.GRAY);
        lines.add(SaltIcon.prefix(SaltIcon.forRole(roleKey).tinted(element.color()), line));
    }
}
