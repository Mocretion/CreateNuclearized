package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayList;
import java.util.List;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineRotorBlock extends RotatedPillarKineticBlock implements IBE<TurbineRotorBlockEntity> {

    private final int radius;
    private final int hitboxRadius;

    public TurbineRotorBlock(Properties properties, int radius, int hitboxRadius) {
        super(properties);
        this.radius = radius;
        this.hitboxRadius = hitboxRadius;
    }

    public int radius() {
        return radius;
    }

    public int hitboxRadius() {
        return hitboxRadius;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public Axis getAxisForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        return base != null ? base.getValue(AXIS) : context.getClickedFace().getAxis();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        Axis axis = state.getValue(AXIS);
        BlockPos pos = context.getClickedPos();
        for (BlockPos off : hitboxOffsets(axis)) {
            if (off.equals(BlockPos.ZERO)) continue;
            if (!context.getLevel().getBlockState(pos.offset(off)).canBeReplaced()) return null;
        }
        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) return;
        Axis axis = state.getValue(AXIS);
        BlockState filler = CNBlocks.TURBINE_ROTOR_STRUCTURAL.getDefaultState();
        for (BlockPos off : hitboxOffsets(axis)) {
            if (off.equals(BlockPos.ZERO)) continue;
            BlockPos cell = pos.offset(off);
            if (level.getBlockState(cell).canBeReplaced())
                level.setBlockAndUpdate(cell, filler.setValue(TurbineRotorStructuralBlock.FACING, towardCenter(off)));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && !level.isClientSide) {
            Axis axis = state.getValue(AXIS);
            for (BlockPos off : hitboxOffsets(axis)) {
                if (off.equals(BlockPos.ZERO)) continue;
                BlockPos cell = pos.offset(off);
                if (level.getBlockState(cell).getBlock() instanceof TurbineRotorStructuralBlock)
                    level.removeBlock(cell, false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<TurbineRotorBlockEntity> getBlockEntityClass() {
        return TurbineRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineRotorBlockEntity> getBlockEntityType() {
        return CNBlockEntities.TURBINE_ROTOR.get();
    }

    private List<BlockPos> hitboxOffsets(Axis axis) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int u = -hitboxRadius; u <= hitboxRadius; u++) {
            for (int v = -hitboxRadius; v <= hitboxRadius; v++) {
                offsets.add(switch (axis) {
                    case X -> new BlockPos(0, u, v);
                    case Y -> new BlockPos(u, 0, v);
                    case Z -> new BlockPos(u, v, 0);
                });
            }
        }
        return offsets;
    }

    private static Direction towardCenter(BlockPos off) {
        int ax = Math.abs(off.getX()), ay = Math.abs(off.getY()), az = Math.abs(off.getZ());
        if (ax >= ay && ax >= az && off.getX() != 0)
            return off.getX() > 0 ? Direction.WEST : Direction.EAST;
        if (az >= ay && off.getZ() != 0)
            return off.getZ() > 0 ? Direction.NORTH : Direction.SOUTH;
        return off.getY() > 0 ? Direction.DOWN : Direction.UP;
    }
}
