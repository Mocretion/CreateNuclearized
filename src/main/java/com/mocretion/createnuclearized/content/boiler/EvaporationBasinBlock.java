package com.mocretion.createnuclearized.content.boiler;

import java.util.EnumMap;
import java.util.Map;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

public class EvaporationBasinBlock extends Block implements IBE<EvaporationBasinBlockEntity>, IWrenchable {

    private static final int WALL = 2;
    private static final Map<CellSlot, VoxelShape> SHAPES = new EnumMap<>(CellSlot.class);

    static {
        for (CellSlot slot : CellSlot.values())
            SHAPES.put(slot, shapeFor(slot));
    }

    public EvaporationBasinBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CellSlot.PROPERTY, CellSlot.SINGLE));
    }

    private static VoxelShape shapeFor(CellSlot slot) {
        double west = slot.isOuter(Direction.WEST) ? WALL : 0;
        double east = slot.isOuter(Direction.EAST) ? 16 - WALL : 16;
        double north = slot.isOuter(Direction.NORTH) ? WALL : 0;
        double south = slot.isOuter(Direction.SOUTH) ? 16 - WALL : 16;
        return Shapes.join(Shapes.block(), Block.box(west, 15, north, east, 16, south), BooleanOp.ONLY_FIRST);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CellSlot.PROPERTY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(CellSlot.PROPERTY));
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) return;
        withBlockEntityDo(level, pos, EvaporationBasinBlockEntity::updateConnectivity);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
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
        EvaporationBasinBlockEntity basin = state.is(newState.getBlock()) ? null : getBlockEntity(level, pos);
        IBE.onRemove(state, level, pos, newState);
        if (basin != null)
            ConnectivityHandler.splitMulti(basin);
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
