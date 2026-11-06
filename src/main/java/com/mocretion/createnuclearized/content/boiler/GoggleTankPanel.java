package com.mocretion.createnuclearized.content.boiler;

import java.util.List;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public final class GoggleTankPanel {

    private GoggleTankPanel() {}

    public static void header(List<Component> tooltip) {
        CreateLang.translate("gui.goggles.fluid_container").forGoggles(tooltip);
    }

    public static void append(List<Component> tooltip, String labelKey, FluidStack stack, int capacity) {
        new LangBuilder(CreateNuclearized.MODID).translate(labelKey)
                .style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        if (stack.isEmpty()) {
            CreateLang.translate("gui.goggles.fluid_container.capacity")
                    .add(CreateLang.number(capacity).add(mb).style(ChatFormatting.GOLD))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 2);
            return;
        }
        CreateLang.fluidName(stack).style(ChatFormatting.GRAY).forGoggles(tooltip, 2);
        CreateLang.builder()
                .add(CreateLang.number(stack.getAmount()).add(mb).style(ChatFormatting.GOLD))
                .text(ChatFormatting.GRAY, " / ")
                .add(CreateLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 2);
    }
}
