package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class TurbineStatorBlock extends DirectionalBlock {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new PlacementHelper());
    private static final int MAX_GROUP_SIZE = 1024;

    public TurbineStatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, OPEN));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this))
            updateGroup(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos,
                                   boolean movedByPiston) {
        updateGroup(level, pos);
    }

    private static void updateGroup(Level level, BlockPos origin) {
        if (level.isClientSide) return;
        List<BlockPos> group = connectedStators(level, origin);
        boolean open = group.stream().anyMatch(level::hasNeighborSignal);
        boolean changed = false;
        for (BlockPos pos : group) {
            BlockState state = level.getBlockState(pos);
            if (state.getValue(OPEN) == open) continue;
            level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
            changed = true;
        }
        if (!changed) return;
        level.playSound(null, origin, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS,
                1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, origin);
    }

    private static List<BlockPos> connectedStators(Level level, BlockPos origin) {
        List<BlockPos> group = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(Set.of(origin));
        Deque<BlockPos> queue = new ArrayDeque<>(List.of(origin));
        while (!queue.isEmpty() && group.size() < MAX_GROUP_SIZE) {
            BlockPos pos = queue.poll();
            group.add(pos);
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (seen.add(next) && level.getBlockState(next).getBlock() instanceof TurbineStatorBlock)
                    queue.add(next);
            }
        }
        return group;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction looking = context.getNearestLookingDirection();
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isShiftKeyDown();
        return defaultBlockState().setValue(FACING, sneaking ? looking.getOpposite() : looking);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.mayBuild()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
        if (!helper.matchesItem(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return helper.getOffset(player, level, state, pos, hitResult)
                .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(TurbineStatorBlock::new);
    }

    private static class PlacementHelper implements IPlacementHelper {

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return stack -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof TurbineStatorBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return state -> state.getBlock() instanceof TurbineStatorBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
            Direction facing = state.getValue(FACING);
            for (Direction side : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facing.getAxis())) {
                BlockPos target = pos.relative(side);
                if (level.getBlockState(target).canBeReplaced())
                    return PlacementOffset.success(target, placed -> placed.setValue(FACING, facing));
            }
            return PlacementOffset.fail();
        }
    }
}
