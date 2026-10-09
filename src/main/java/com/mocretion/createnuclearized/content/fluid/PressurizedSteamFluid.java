package com.mocretion.createnuclearized.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class PressurizedSteamFluid extends BaseFlowingFluid {

    private static final int FLOW_SPEED = 1;
    private static final int MAX_SIDEWAYS_AMOUNT = 7;

    protected PressurizedSteamFluid(Properties properties) {
        super(properties);
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return FLOW_SPEED;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 1;
    }

    @Override
    public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return false;
    }

    @Override
    protected void spread(Level level, BlockPos pos, FluidState state) {
        if (state.isEmpty()) return;
        BlockState here = level.getBlockState(pos);
        BlockPos up = pos.above();
        BlockState upState = level.getBlockState(up);
        FluidState upFluid = level.getFluidState(up);
        FluidState newUp = getNewLiquid(level, up, upState);

        boolean rose = canSpreadTo(level, pos, here, Direction.UP, up, upState, upFluid, newUp.getType());
        if (rose)
            spreadTo(level, up, upState, Direction.UP, newUp);

        boolean blocked = !rose && !upFluid.getType().isSame(this);
        if (state.isSource() || blocked)
            spreadSideways(level, pos, here);
    }

    @Override
    protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState state) {
        int feed = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos np = pos.relative(dir);
            BlockState ns = level.getBlockState(np);
            FluidState nf = ns.getFluidState();
            if (nf.getType().isSame(this) && canPassThroughWall(dir, level, pos, state, np, ns))
                feed = Math.max(feed, nf.getAmount());
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        FluidState belowFluid = belowState.getFluidState();
        if (!belowFluid.isEmpty() && belowFluid.getType().isSame(this)
                && canPassThroughWall(Direction.DOWN, level, pos, state, below, belowState)) {
            return getFlowing(8, true);
        }

        FluidState current = state.getFluidState();
        int currentAmount = current.getType().isSame(this) ? current.getAmount() : 0;
        if (feed > currentAmount) {
            int newAmount = Math.min(feed - getDropOff(level), MAX_SIDEWAYS_AMOUNT);
            if (newAmount > 0) return getFlowing(newAmount, false);
        }
        return Fluids.EMPTY.defaultFluidState();
    }

    private void spreadSideways(Level level, BlockPos pos, BlockState blockState) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos np = pos.relative(dir);
            BlockState ns = level.getBlockState(np);
            FluidState newFluid = getNewLiquid(level, np, ns);
            if (!newFluid.isEmpty()
                    && canSpreadTo(level, pos, blockState, dir, np, ns, level.getFluidState(np), newFluid.getType())) {
                spreadTo(level, np, ns, dir, newFluid);
            }
        }
    }

    private static boolean canPassThroughWall(Direction dir, BlockGetter level, BlockPos pos, BlockState state,
                                              BlockPos spreadPos, BlockState spreadState) {
        VoxelShape shape = state.getCollisionShape(level, pos);
        VoxelShape spreadShape = spreadState.getCollisionShape(level, spreadPos);
        return !Shapes.mergedFaceOccludes(shape, spreadShape, dir);
    }

    public static class Flowing extends PressurizedSteamFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends PressurizedSteamFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
