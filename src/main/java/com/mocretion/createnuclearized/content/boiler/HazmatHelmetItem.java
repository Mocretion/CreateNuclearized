package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.registry.CNBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class HazmatHelmetItem extends ArmorItem {

    public HazmatHelmetItem(Properties properties) {
        super(ArmorMaterials.GOLD, ArmorItem.Type.HELMET, properties);
    }

    // Right clicking a lit blaze burner caps the blaze with the helmet, turning it into a contained blaze burner
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(AllBlocks.BLAZE_BURNER.get())) return InteractionResult.PASS;
        // Needs an actual blaze in there to wear it
        if (state.getValue(BlazeBurnerBlock.HEAT_LEVEL) == HeatLevel.NONE) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        level.setBlockAndUpdate(pos, CNBlocks.CONTAINED_BLAZE_BURNER.get().defaultBlockState());
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.BLOCKS, 1.0f, 1.0f);

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isCreative())
            stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
