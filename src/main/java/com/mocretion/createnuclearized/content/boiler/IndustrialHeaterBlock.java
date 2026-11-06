package com.mocretion.createnuclearized.content.boiler;

import java.util.Locale;

import com.mocretion.createnuclearized.registry.CNBlockEntities;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class IndustrialHeaterBlock extends Block implements IBE<IndustrialHeaterBlockEntity>, IWrenchable {

    public enum Temperature implements StringRepresentable {
        LOW(1, 1),
        MID(2, 2),
        HIGH(5, 4);

        public final float contactDamage;
        public final double contactHeatLoss;

        Temperature(float contactDamage, double contactHeatLoss) {
            this.contactDamage = contactDamage;
            this.contactHeatLoss = contactHeatLoss;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<HeatLevel> HEAT_LEVEL =
            EnumProperty.create("blaze", HeatLevel.class, HeatLevel.SMOULDERING, HeatLevel.KINDLED);

    public static final EnumProperty<Temperature> TEMPERATURE =
            EnumProperty.create("temperature", Temperature.class);

    public IndustrialHeaterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HEAT_LEVEL, HeatLevel.SMOULDERING)
                .setValue(TEMPERATURE, Temperature.LOW)
                .setValue(CellSlot.PROPERTY, CellSlot.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL, TEMPERATURE, CellSlot.PROPERTY);
    }

    public static HeatLevel heatLevel(boolean active) {
        return active ? HeatLevel.KINDLED : HeatLevel.SMOULDERING;
    }

    public static int getLight(BlockState state) {
        return state.getValue(HEAT_LEVEL) == HeatLevel.SMOULDERING ? 8 : 15;
    }

    public static Temperature temperatureFor(double fillFraction) {
        if (fillFraction <= 0.10) return Temperature.LOW;
        if (fillFraction <= 0.75) return Temperature.MID;
        return Temperature.HIGH;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock()) return;
        if (moved) return;
        withBlockEntityDo(world, pos, IndustrialHeaterBlockEntity::updateConnectivity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IndustrialHeaterBlockEntity heaterBE = state.is(newState.getBlock()) ? null : getBlockEntity(level, pos);
        IBE.onRemove(state, level, pos, newState);
        if (heaterBE != null) ConnectivityHandler.splitMulti(heaterBE);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        IndustrialHeaterBlockEntity be = getBlockEntity(level, pos);
        return be == null ? 0 : be.comparatorSignal();
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
