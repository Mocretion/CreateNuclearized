package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.mocretion.createnuclearized.content.controlrod.ControlRodBlockEntity;
import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.multiblock.MultiblockController;
import com.mocretion.createnuclearized.content.multiblock.MultiblockErrorPayload;
import com.mocretion.createnuclearized.content.multiblock.MultiblockState;
import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider, MultiblockController {

    private static final int TICKS_PER_SECOND = 20;

    @Nullable
    private ReactorMultiblock.Structure structure;
    private List<ReactorStats.Channel> channels = List.of();
    private List<ReactorStats.Channel> producing = List.of();
    private Map<Long, List<ReactorStats.ModEntry>> channelMods = Map.of();
    private final Set<BlockPos> activeValves = new HashSet<>();
    private final Set<BlockPos> fullValves = new HashSet<>();
    private int reflectorPercent;
    private final Map<ReactorFuel, SaltStats> compositions = new HashMap<>();
    private ReactorFuel fuel = ReactorFuel.URANIUM;

    private boolean running;
    private int timer = TICKS_PER_SECOND;
    private MultiblockState state = MultiblockState.STOPPED;

    private int activeChannels;
    private long runTicks;

    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public MultiblockState getState() {
        return state;
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

    public InteriorGrid buildInteriorGrid() {
        ReactorMultiblock.Structure reactor = structure;
        if (reactor == null) return InteriorGrid.EMPTY;
        BlockPos min = reactor.min();
        BlockPos max = reactor.max();
        BlockPos origin = new BlockPos(min.getX() + 1, min.getY(), min.getZ() + 1);
        List<InteriorGrid.Cell> cells = new ArrayList<>();
        for (ReactorStats.Channel channel : channels) {
            BlockPos valve = channel.valve();
            List<InteriorGrid.Mod> mods = new ArrayList<>();
            for (ReactorStats.ModEntry mod : channelMods.getOrDefault(ReactorMultiblock.column(valve), List.of()))
                mods.add(new InteriorGrid.Mod(BuiltInRegistries.BLOCK.getKey(mod.block()), mod.count()));
            cells.add(new InteriorGrid.Cell(valve.getX() - origin.getX(), valve.getZ() - origin.getZ(),
                    valveState(valve), channel.burnShift(), channel.purityShift(), channel.rate(), mods));
        }
        return new InteriorGrid(max.getX() - min.getX() - 1, max.getZ() - min.getZ() - 1,
                origin, fuel.index(), reflectorPercent, cells);
    }

    private InteriorGrid.ValveState valveState(BlockPos valve) {
        if (activeValves.contains(valve)) return InteriorGrid.ValveState.ACTIVE;
        if (fullValves.contains(valve)) return InteriorGrid.ValveState.FULL;
        return InteriorGrid.ValveState.INACTIVE;
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
        return sumValves(outputValves(), ReactorFluidValveBlockEntity::getHeatedCapacity);
    }

    public int getRunSeconds() {
        return (int) (runTicks / TICKS_PER_SECOND);
    }

    @Override
    public void toggleRunning(@Nullable Player player) {
        if (level == null) return;
        if (running) {
            running = false;
            state = MultiblockState.STOPPED;
            activeChannels = 0;
            syncStateToBlock();
            setChanged();
            return;
        }
        ReactorMultiblock.Result result = ReactorMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            clearStructure();
            if (player != null) {
                Component message = Component.translatable(CreateNuclearized.MODID + ".multiblock.reactor.invalid", result.reason());
                player.displayClientMessage(message, true);
                if (player instanceof ServerPlayer serverPlayer && result.at() != null)
                    MultiblockErrorPayload.send(serverPlayer, result.at());
            }
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
        boolean validate = ++timer >= TICKS_PER_SECOND;
        if (validate) {
            timer = 0;
            revalidate();
        }
        ReactorMultiblock.Structure reactor = structure;
        if (running && reactor != null) {
            driveRods(reactor);
            tickConversion(reactor);
        } else {
            activeChannels = 0;
            activeValves.clear();
            fullValves.clear();
        }
        if (validate)
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
            state = MultiblockState.STOPPED;
            return;
        }
        applyStructure(result.structure());
        if (!running)
            state = MultiblockState.STOPPED;
    }

    private void applyStructure(ReactorMultiblock.Structure next) {
        clearValveStatsExcept(next.outputValves());
        structure = next;
        ReactorStats.Layout layout = ReactorStats.scan(level, next);
        channels = layout.channels();
        channelMods = layout.modifiers();
        reflectorPercent = layout.reflectorPercent();
        updateCompositions(producingChannels(next));
        fuel = detectFuel();
        pushChannelStats();
    }

    private void updateCompositions(List<ReactorStats.Channel> nowProducing) {
        producing = nowProducing;
        List<ReactorStats.Channel> basis = nowProducing.isEmpty() ? channels : nowProducing;
        for (ReactorFuel candidate : ReactorFuel.values())
            compositions.put(candidate, ReactorStats.composition(basis, candidate, reflectorPercent));
    }

    private List<ReactorStats.Channel> producingChannels(ReactorMultiblock.Structure reactor) {
        List<ReactorStats.Channel> open = new ArrayList<>();
        for (ReactorStats.Channel channel : channels) {
            if (channel.rate() > 0 && channelOpen(reactor, channel.valve()))
                open.add(channel);
        }
        return open;
    }

    private void clearStructure() {
        clearValveStats();
        structure = null;
        channels = List.of();
        producing = List.of();
        channelMods = Map.of();
        activeValves.clear();
        fullValves.clear();
        reflectorPercent = 0;
        compositions.clear();
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
        List<ReactorStats.Channel> nowProducing = producingChannels(reactor);
        if (!nowProducing.equals(producing))
            updateCompositions(nowProducing);
        activeValves.clear();
        fullValves.clear();
        int active = 0;
        for (ReactorStats.Channel channel : nowProducing) {
            ReactorFluidValveBlockEntity valve = valveAt(channel.valve());
            if (valve == null) continue;
            ReactorFuel valveFuel = valve.moltenFuel();
            if (valveFuel == null) continue;
            valve.advanceRate();
            convert(valve, valveFuel);
            if (hasRoomFor(valve, valveFuel)) {
                active++;
                activeValves.add(channel.valve());
            } else {
                fullValves.add(channel.valve());
            }
        }
        activeChannels = active;
        state = active > 0 ? MultiblockState.RUNNING : MultiblockState.IDLE;
        runTicks++;
    }

    private void convert(ReactorFluidValveBlockEntity valve, ReactorFuel valveFuel) {
        int want = Math.min(valve.pendingWholeMb(), valve.getMoltenAmount());
        if (want <= 0) return;
        FluidStack spent = spentSalt(valveFuel, want);
        int converted = valve.fillHeated(spent, FluidAction.SIMULATE);
        if (converted <= 0) return;
        spent.setAmount(converted);
        valve.getMoltenTank().drain(converted, FluidAction.EXECUTE);
        valve.fillHeated(spent, FluidAction.EXECUTE);
        valve.consumePending(converted);
        valve.notifyContentsChanged();
    }

    private boolean hasRoomFor(ReactorFluidValveBlockEntity valve, ReactorFuel valveFuel) {
        return valve.fillHeated(spentSalt(valveFuel, 1), FluidAction.SIMULATE) > 0;
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
