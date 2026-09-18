package com.mocretion.createnuclearized.content.turbine;

import java.util.function.Consumer;

import com.mocretion.createnuclearized.registry.CNBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.HitResult;

import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public class TurbineRotorStructuralBlock extends DirectionalBlock {

    public TurbineRotorStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Solid for collision but fully transparent to light, so the invisible hitbox cells dont cast shadows into the rotor
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        BlockPos master = getMaster(level, pos, state);
        return level.getBlockState(master).getBlock().getCloneItemStack(level, master, level.getBlockState(master));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Break the whole rotor when a hitbox cell goes, but drop it only in survival (creative shouldnt spit out the item)
        if (!movedByPiston && !state.is(newState.getBlock()) && stillValid(level, pos, state))
            level.destroyBlock(getMaster(level, pos, state), true);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Punching a hitbox cell removes the rotor; suppress the item in creative like Create's water wheel does
        if (stillValid(level, pos, state)) {
            BlockPos master = getMaster(level, pos, state);
            if (!level.isClientSide() && player.isCreative())
                level.destroyBlock(master, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (level instanceof Level realLevel && !realLevel.isClientSide()
                && !level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!stillValid(level, pos, state))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    // Walk FACING until it lands on the rotor block, that pos is the master
    public static BlockPos getMaster(BlockGetter level, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FACING);
        BlockPos next = pos.relative(dir);
        BlockState nextState = level.getBlockState(next);
        if (nextState.getBlock() instanceof TurbineRotorStructuralBlock)
            return getMaster(level, next, nextState);
        return next;
    }

    // The FACING chain has to reach a rotor whose axis is perpendicular to this filler plane
    public static boolean stillValid(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof TurbineRotorStructuralBlock)) return false;
        Direction dir = state.getValue(FACING);
        BlockPos next = pos.relative(dir);
        BlockState nextState = level.getBlockState(next);
        if (nextState.getBlock() instanceof TurbineRotorStructuralBlock)
            return stillValid(level, next, nextState);
        return CNBlocks.isTurbineRotor(nextState)
                && nextState.getValue(TurbineRotorBlock.AXIS) != dir.getAxis();
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return false;
    }

    // Invisible, so kill the fallback (missing texture) landing particles
    @Override
    public boolean addLandingEffects(BlockState state, ServerLevel level, BlockPos pos, BlockState state2,
                                     net.minecraft.world.entity.LivingEntity entity, int numberOfParticles) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target,
                                         net.minecraft.client.particle.ParticleEngine manager) {
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos,
                                             net.minecraft.client.particle.ParticleEngine manager) {
                return true;
            }
        });
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(TurbineRotorStructuralBlock::new);
    }
}
