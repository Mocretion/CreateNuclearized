package com.mocretion.createnuclearized.content.fluid;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

public class SpentSaltBucketItem extends BucketItem {

    public SpentSaltBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack containerStack, BlockPos pos) {
        super.checkExtraContent(player, level, containerStack, pos);
        if (level.isClientSide) return;
        SaltStats stats = containerStack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null && level.getBlockEntity(pos) instanceof SpentSaltBlockEntity be)
            be.setStats(stats);
    }
}
