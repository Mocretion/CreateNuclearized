package com.mocretion.createnuclearized.content.turbine;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CreateNuclearized.MODID, value = Dist.CLIENT)
public final class TurbineTooltips {

    private TurbineTooltips() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof TurbineRotorBlock rotor) {
            event.getToolTip().add(Component.translatable("createnuclearized.tooltip.turbine_rotor.steam", rotor.steamPerTick())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
