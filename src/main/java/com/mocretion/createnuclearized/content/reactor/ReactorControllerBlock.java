package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ReactorControllerBlock extends Block implements EntityBlock {

    // The face with the status panel; the other five are casing
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    // Status tile on the FACING face; written by the block entity
    public static final EnumProperty<ReactorState> STATE = EnumProperty.create("state", ReactorState.class);

    public ReactorControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATE, ReactorState.STOPPED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Panel faces the player who placed it (like a dispensers front)
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity be)
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CNBlockEntities.REACTOR_CONTROLLER.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ReactorControllerBlockEntity controller)
                controller.serverTick();
        };
    }
}
