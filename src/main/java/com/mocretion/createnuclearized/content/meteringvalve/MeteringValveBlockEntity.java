package com.mocretion.createnuclearized.content.meteringvalve;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class MeteringValveBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 1000;
    private static final int DEFAULT_DOSE = 100;
    private static final int FINE_MAX = 100;
    private static final int COARSE_STEP = CAPACITY / FINE_MAX;

    private SmartFluidTankBehaviour tank;
    private ScrollValueBehaviour dose;

    public MeteringValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, CAPACITY);
        dose = new DoseBehaviour(this);
        dose.setValue(DEFAULT_DOSE);
        behaviours.add(tank);
        behaviours.add(dose);
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == facing()) return null;
        return tank.getCapability();
    }

    public int getDose() {
        return dose.getValue();
    }

    public void dispense() {
        Direction facing = facing();
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(facing), facing.getOpposite());
        if (target == null) return;
        IFluidHandler source = tank.getPrimaryHandler();
        int amount = getDose();
        FluidStack available = source.drain(amount, FluidAction.SIMULATE);
        if (available.getAmount() < amount || target.fill(available, FluidAction.SIMULATE) < amount) return;
        target.fill(source.drain(amount, FluidAction.EXECUTE), FluidAction.EXECUTE);
        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
    }

    private Direction facing() {
        return getBlockState().getValue(MeteringValveBlock.FACING);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        new LangBuilder(CreateNuclearized.MODID).translate("gui.goggles.metering_dose", getDose()).forGoggles(tooltip);
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
        return true;
    }

    private static class DoseBehaviour extends ScrollValueBehaviour {

        DoseBehaviour(SmartBlockEntity be) {
            super(Component.translatable("createnuclearized.metering_valve.dose"), be,
                    new CenteredSideValueBoxTransform((state, dir) -> dir.getAxis() != state.getValue(MeteringValveBlock.FACING).getAxis()));
            between(1, CAPACITY);
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hit) {
            return new ValueSettingsBoard(label, FINE_MAX, 10,
                    List.of(Component.translatable("createnuclearized.metering_valve.fine"),
                            Component.translatable("createnuclearized.metering_valve.coarse")),
                    new ValueSettingsFormatter(settings -> Component.translatable("createnuclearized.metering_valve.amount",
                            doseFor(settings))));
        }

        @Override
        public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
            if (!settings.equals(getValueSettings()))
                playFeedbackSound(this);
            setValue(doseFor(settings));
        }

        @Override
        public ValueSettings getValueSettings() {
            return value <= FINE_MAX ? new ValueSettings(0, value) : new ValueSettings(1, value / COARSE_STEP);
        }

        private static int doseFor(ValueSettings settings) {
            return Math.max(1, settings.row() == 0 ? settings.value() : settings.value() * COARSE_STEP);
        }
    }
}
