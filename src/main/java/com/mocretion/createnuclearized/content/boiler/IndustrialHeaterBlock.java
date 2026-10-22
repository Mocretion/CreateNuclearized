package com.mocretion.createnuclearized.content.boiler;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class IndustrialHeaterBlock extends Block implements IBE<IndustrialHeaterBlockEntity> {

    public static final EnumProperty<HeatLevel> HEAT_LEVEL =
            EnumProperty.create("blaze", HeatLevel.class, HeatLevel.SMOULDERING, HeatLevel.KINDLED);

    public IndustrialHeaterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, HeatLevel.SMOULDERING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL);
    }

    public static HeatLevel heatLevel(boolean active) {
        return active ? HeatLevel.KINDLED : HeatLevel.SMOULDERING;
    }

    public static int getLight(BlockState state) {
        return state.getValue(HEAT_LEVEL) == HeatLevel.SMOULDERING ? 8 : 15;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<IndustrialHeaterBlockEntity> getBlockEntityClass() {
        return IndustrialHeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IndustrialHeaterBlockEntity> getBlockEntityType() {
        return CNBlockEntities.INDUSTRIAL_HEATER.get();
    }
}
