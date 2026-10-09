package com.mocretion.createnuclearized.content.reactor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.mocretion.createnuclearized.content.controlrod.ControlRodBlockEntity;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider {

    private static final int TICKS_PER_SECOND = 20;

    @Nullable
    private ReactorMultiblock.Structure structure;
    private List<ReactorStats.Channel> channels = List.of();
    private final Map<ReactorFuel, SaltStats> compositions = new EnumMap<>(ReactorFuel.class);
    private ReactorFuel fuel = ReactorFuel.URANIUM;

    private boolean running;
    private int timer = TICKS_PER_SECOND;
    private ReactorState state = ReactorState.STOPPED;

    private int flowPerTick;
    private int activeChannels;
    private long runTicks;

    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isRunning() {
        return running;
    }

    public ReactorState getState() {
        return state;
    }

    public int getStateOrdinal() {
        return state.ordinal();
    }

    public int getFlowPerTick() {
        return flowPerTick;
    }

    public int getActiveChannels() {
        return activeChannels;
    }

    public int getTotalChannels() {
        return channels.size();
    }

    public SaltStats getOutputStats() {
        return compositionFor(fuel);
    }

    public ReactorFuel getFuel() {
        return fuel;
    }

    public int getTotalRateMilli() {
        double sum = 0;
        for (ReactorStats.Channel channel : channels)
            sum += channel.rate();
        return (int) Math.round(sum * 1000);
    }

    public int getHeatedBuffered() {
        return sumValves(outputValves(), ReactorFluidValveBlockEntity::getHeatedAmount);
    }

    public int getFissileCapacity() {
        return sumValves(allValves(), valve -> valve.getMoltenTank().getCapacity());
    }

    public int getFissileBuffered() {
        return sumValves(allValves(), ReactorFluidValveBlockEntity::getMoltenAmount);
    }

    public int getSpentCapacity() {
        return sumValves(outputValves(), valve -> valve.getHeatedTank().getCapacity());
    }

    public int getRunSeconds() {
        return (int) (runTicks / TICKS_PER_SECOND);
    }

    public void setRunningFromNetwork(boolean running) {
        this.running = running;
    }

    public void setStateFromNetwork(int ordinal) {
        this.state = ReactorState.byOrdinal(ordinal);
    }

    public void toggleRunning(@Nullable Player player) {
        if (level == null) return;
        if (running) {
            running = false;
            state = ReactorState.STOPPED;
            flowPerTick = 0;
            activeChannels = 0;
            setChanged();
            return;
        }
        ReactorMultiblock.Result result = ReactorMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            clearStructure();
            if (player != null)
                player.displayClientMessage(Component.literal("Reactor invalid: " + result.reason()), true);
            return;
        }
        applyStructure(result.structure());
        runTicks = 0;
        running = true;
        setChanged();
    }

    public void clearValveStats() {
        clearValveStatsExcept(List.of());
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (++timer >= TICKS_PER_SECOND) {
            timer = 0;
            revalidate();
        }
        ReactorMultiblock.Structure reactor = structure;
        if (running && reactor != null) {
            driveRods(reactor);
            tickConversion(reactor);
        } else {
            flowPerTick = 0;
            activeChannels = 0;
        }
        syncStateToBlock();
    }

    private void syncStateToBlock() {
        if (level == null) return;
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(ReactorControllerBlock.STATE)
                && blockState.getValue(ReactorControllerBlock.STATE) != state) {
            level.setBlock(worldPosition, blockState.setValue(ReactorControllerBlock.STATE, state),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void revalidate() {
        ReactorMultiblock.Result result = ReactorMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            if (structure != null || running) {
                running = false;
                clearStructure();
                setChanged();
            }
            state = ReactorState.STOPPED;
            return;
        }
        applyStructure(result.structure());
        if (!running)
            state = ReactorState.STOPPED;
    }

    private void applyStructure(ReactorMultiblock.Structure next) {
        clearValveStatsExcept(next.outputValves());
        structure = next;
        channels = ReactorStats.channels(level, next);
        for (ReactorFuel candidate : ReactorFuel.values())
            compositions.put(candidate, ReactorStats.composition(channels, candidate));
        fuel = detectFuel();
        pushChannelStats();
    }

    private void clearStructure() {
        clearValveStats();
        structure = null;
        channels = List.of();
        compositions.clear();
        flowPerTick = 0;
        activeChannels = 0;
    }

    private void clearValveStatsExcept(List<BlockPos> kept) {
        for (BlockPos pos : outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve != null && !kept.contains(pos))
                valve.clearStats();
        }
    }

    private void pushChannelStats() {
        for (ReactorStats.Channel channel : channels) {
            ReactorFluidValveBlockEntity valve = valveAt(channel.valve());
            if (valve == null) continue;
            ReactorFuel shown = Objects.requireNonNullElse(valve.displayFuel(), fuel);
            valve.setStats(channel.burn(shown), channel.purity(shown), channel.rate());
        }
    }

    private ReactorFuel detectFuel() {
        for (BlockPos pos : allValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            ReactorFuel found = valve == null ? null : valve.moltenFuel();
            if (found != null) return found;
        }
        return ReactorFuel.URANIUM;
    }

    private SaltStats compositionFor(ReactorFuel saltFuel) {
        return compositions.getOrDefault(saltFuel, SaltStats.EMPTY);
    }

    private void tickConversion(ReactorMultiblock.Structure reactor) {
        int flow = 0;
        int active = 0;
        for (BlockPos pos : reactor.outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve == null || !channelOpen(reactor, pos)) continue;
            valve.advanceRate();
            ReactorFuel valveFuel = valve.moltenFuel();
            if (valveFuel == null) continue;
            flow += convert(valve, valveFuel);
            if (valve.getRate() > 0 && hasRoomFor(valve, valveFuel))
                active++;
        }
        flowPerTick = flow;
        activeChannels = active;
        state = active > 0 ? ReactorState.RUNNING : ReactorState.IDLE;
        runTicks++;
    }

    private int convert(ReactorFluidValveBlockEntity valve, ReactorFuel valveFuel) {
        int want = Math.min(valve.pendingWholeMb(), valve.getMoltenAmount());
        if (want <= 0) return 0;
        FluidStack spent = spentSalt(valveFuel, want);
        int converted = valve.getHeatedTank().fill(spent, FluidAction.SIMULATE);
        if (converted <= 0) return 0;
        spent.setAmount(converted);
        valve.getMoltenTank().drain(converted, FluidAction.EXECUTE);
        valve.getHeatedTank().fill(spent, FluidAction.EXECUTE);
        valve.consumePending(converted);
        valve.notifyContentsChanged();
        return converted;
    }

    private boolean hasRoomFor(ReactorFluidValveBlockEntity valve, ReactorFuel valveFuel) {
        return valve.getHeatedTank().fill(spentSalt(valveFuel, 1), FluidAction.SIMULATE) > 0;
    }

    private FluidStack spentSalt(ReactorFuel saltFuel, int amount) {
        FluidStack stack = new FluidStack(saltFuel.outputFluid().builtInRegistryHolder(), amount);
        stack.set(CNDataComponents.SALT_STATS.get(), compositionFor(saltFuel));
        return stack;
    }

    private void driveRods(ReactorMultiblock.Structure reactor) {
        for (BlockPos pos : reactor.controlRods()) {
            if (level.getBlockEntity(pos) instanceof ControlRodBlockEntity rod)
                rod.engage();
        }
    }

    private boolean channelOpen(ReactorMultiblock.Structure reactor, BlockPos valvePos) {
        BlockPos rodPos = valvePos.atY(reactor.max().getY());
        return level.getBlockEntity(rodPos) instanceof ControlRodBlockEntity rod && rod.isRaised();
    }

    private List<BlockPos> outputValves() {
        return structure == null ? List.of() : structure.outputValves();
    }

    private Iterable<BlockPos> allValves() {
        return structure == null ? List.of() : Iterables.concat(structure.outputValves(), structure.inputValves());
    }

    @Nullable
    private ReactorFluidValveBlockEntity valveAt(BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof ReactorFluidValveBlockEntity valve ? valve : null;
    }

    private int sumValves(Iterable<BlockPos> positions, ToIntFunction<ReactorFluidValveBlockEntity> value) {
        int total = 0;
        for (BlockPos pos : positions) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve != null)
                total += value.applyAsInt(valve);
        }
        return total;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorControllerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Running", running);
        tag.putLong("RunTicks", runTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        runTicks = tag.getLong("RunTicks");
    }
}
