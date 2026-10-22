package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

public class EvaporationBasinBlock extends Block implements IBE<EvaporationBasinBlockEntity> {


    private static final VoxelShape SHAPE = new AllShapes.Builder(Block.box(0, 0, 0, 16, 16, 16))
            .erase(2, 0, 2, 14, 16, 14)
            .add(2, 9, 2, 14, 15,14).build();

    public EvaporationBasinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<EvaporationBasinBlockEntity> getBlockEntityClass() {
        return EvaporationBasinBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EvaporationBasinBlockEntity> getBlockEntityType() {
        return CNBlockEntities.EVAPORATION_BASIN.get();
    }
}
