package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ExhaustHoodBlock extends Block implements IBE<ExhaustHoodBlockEntity> {

    private static final VoxelShape SHAPE = new AllShapes.Builder(Block.box(0, 2, 0, 16, 16, 16)).build();

    public ExhaustHoodBlock(Properties properties) {
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<ExhaustHoodBlockEntity> getBlockEntityClass() {
        return ExhaustHoodBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ExhaustHoodBlockEntity> getBlockEntityType() {
        return CNBlockEntities.EXHAUST_HOOD.get();
    }
}
