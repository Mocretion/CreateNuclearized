package com.mocretion.createnuclearized.content.fluid;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class SpentSaltLiquidBlock extends LiquidBlock implements EntityBlock {

    public SpentSaltLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getFluidState().isSource() ? CNBlockEntities.SPENT_SALT.get().create(pos, state) : null;
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        SaltStats stats = statsAt(level, pos);
        ItemStack bucket = super.pickupBlock(player, level, pos, state);
        if (!bucket.isEmpty() && !stats.isEmpty())
            bucket.set(CNDataComponents.SALT_STATS.get(), stats);
        return bucket;
    }

    public static SaltStats statsAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SpentSaltBlockEntity be ? be.getStats() : SaltStats.EMPTY;
    }
}
