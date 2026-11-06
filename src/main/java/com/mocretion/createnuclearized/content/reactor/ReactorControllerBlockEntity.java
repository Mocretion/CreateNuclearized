package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.mocretion.createnuclearized.content.advancement.CNCriteriaTriggers;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerLevel;
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
    private Map<Long, List<ReactorStats.ModPosEntry>> channelModPositions = Map.of();
    private final Set<BlockPos> activeValves = new HashSet<>();
    private final Set<BlockPos> fullValves = new HashSet<>();
    private final Map<ReactorFuel, SaltStats> compositions = new HashMap<>();
    private final Map<StampKey, SaltStats> stamps = new HashMap<>();
    @Nullable
    private ReactorFuel fuel;
    @Nullable
    private ReactorFuel lastProcessedFuel;

    private boolean running;
    private int timer = TICKS_PER_SECOND;
    private MultiblockState state = MultiblockState.STOPPED;

    private int activeChannels;
    private long runTicks;

    @Nullable
    private UUID starterUuid;
    private final Set<ReactorFuel> triggeredFuels = Collections.newSetFromMap(new IdentityHashMap<>());

    private record StampKey(BlockPos valve, ReactorFuel fuel) {}

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

    public int getSpentDrainCount() {
        return countDrain(ReactorFluidValveBlockEntity.DrainSource.OUTPUT);
    }

    public int getFuelDrainCount() {
        return countDrain(ReactorFluidValveBlockEntity.DrainSource.INPUT);
    }

    private int countDrain(ReactorFluidValveBlockEntity.DrainSource source) {
        int count = 0;
        for (BlockPos pos : outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve != null && valve.getDrainSource() == source)
                count++;
        }
        return count;
    }

    @Nullable
    public SaltStats getOutputStats() {
        return fuel == null ? null : compositionFor(fuel);
    }

    @Nullable
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
            long column = ReactorMultiblock.column(valve);
            List<InteriorGrid.Mod> mods = new ArrayList<>();
            for (ReactorStats.ModEntry mod : channelMods.getOrDefault(column, List.of()))
                mods.add(new InteriorGrid.Mod(BuiltInRegistries.BLOCK.getKey(mod.block()), mod.role(), mod.count()));
            List<InteriorGrid.ModPos> positions = new ArrayList<>();
            for (ReactorStats.ModPosEntry pos : channelModPositions.getOrDefault(column, List.of()))
                positions.add(new InteriorGrid.ModPos(pos.col(), pos.row(),
                        BuiltInRegistries.BLOCK.getKey(pos.block()), pos.role(), pos.count()));
            cells.add(new InteriorGrid.Cell(valve.getX() - origin.getX(), valve.getZ() - origin.getZ(),
                    valveState(valve), channel.burnShift(), channel.purityShift(), channel.rate(),
                    channel.reflectorPercent(), mods, positions));
        }
        return new InteriorGrid(max.getX() - min.getX() - 1, max.getZ() - min.getZ() - 1,
                origin, fuel == null ? -1 : fuel.index(), cells);
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
            starterUuid = null;
            triggeredFuels.clear();
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
        setAllDrainSources(ReactorFluidValveBlockEntity.DrainSource.OUTPUT);
        runTicks = 0;
        running = true;
        triggeredFuels.clear();
        if (player instanceof ServerPlayer serverPlayer) {
            starterUuid = serverPlayer.getUUID();
            CNCriteriaTriggers.REACTOR_STARTED.get().trigger(serverPlayer);
        } else {
            starterUuid = null;
        }
        setChanged();
    }

    public void toggleDrainSources() {
        if (level == null || running) return;
        int spent = 0;
        int fuel = 0;
        for (BlockPos pos : outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve == null) continue;
            if (valve.getDrainSource() == ReactorFluidValveBlockEntity.DrainSource.INPUT) fuel++;
            else spent++;
        }
        if (spent + fuel == 0) return;
        setAllDrainSources(spent <= fuel
                ? ReactorFluidValveBlockEntity.DrainSource.OUTPUT
                : ReactorFluidValveBlockEntity.DrainSource.INPUT);
    }

    private void setAllDrainSources(ReactorFluidValveBlockEntity.DrainSource source) {
        for (BlockPos pos : outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve != null)
                valve.setDrainSource(source);
        }
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
        stamps.clear();
        structure = next;
        ReactorStats.Layout layout = ReactorStats.scan(level, next);
        channels = layout.channels();
        channelMods = layout.modifiers();
        channelModPositions = layout.modifierPositions();
        updateCompositions(producingChannels(next));
        fuel = detectFuel();
        pushChannelStats();
    }

    private void updateCompositions(List<ReactorStats.Channel> nowProducing) {
        producing = nowProducing;
        List<ReactorStats.Channel> basis = nowProducing.isEmpty() ? channels : nowProducing;
        for (ReactorFuel candidate : ReactorFuel.values())
            compositions.put(candidate, ReactorStats.composition(basis, candidate));
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
        channelModPositions = Map.of();
        activeValves.clear();
        fullValves.clear();
        compositions.clear();
        stamps.clear();
        activeChannels = 0;
        fuel = null;
    }

    private void clearValveStatsExcept(List<BlockPos> kept) {
        for (BlockPos pos : outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            if (valve != null && !kept.contains(pos))
                valve.clearStats();
        }
    }

    private void pushChannelStats() {
        ReactorFuel fallback = fuel == null ? ReactorFuel.URANIUM : fuel;
        for (ReactorStats.Channel channel : channels) {
            ReactorFluidValveBlockEntity valve = valveAt(channel.valve());
            if (valve == null) continue;
            ReactorFuel shown = Objects.requireNonNullElse(valve.displayFuel(), fallback);
            valve.setStats(channel.burn(shown), channel.purity(shown), channel.rate());
        }
    }

    @Nullable
    private ReactorFuel detectFuel() {
        for (BlockPos pos : outputValves()) {
            ReactorFluidValveBlockEntity valve = valveAt(pos);
            ReactorFuel found = valve == null ? null : valve.displayFuel();
            if (found != null) return found;
        }
        if (structure != null) {
            for (BlockPos pos : structure.inputValves()) {
                ReactorFluidValveBlockEntity valve = valveAt(pos);
                ReactorFuel found = valve == null ? null : valve.moltenFuel();
                if (found != null) return found;
            }
        }
        return lastProcessedFuel;
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
            convert(valve, channel, valveFuel);
            if (hasRoomFor(valve, channel, valveFuel)) {
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

    private void convert(ReactorFluidValveBlockEntity valve, ReactorStats.Channel channel, ReactorFuel valveFuel) {
        int want = Math.min(valve.pendingWholeMb(), valve.getMoltenAmount());
        if (want <= 0) return;
        FluidStack spent = spentSalt(channel, valveFuel, want);
        int converted = valve.fillHeated(spent, FluidAction.SIMULATE);
        if (converted <= 0) return;
        spent.setAmount(converted);
        valve.getMoltenTank().drain(converted, FluidAction.EXECUTE);
        valve.fillHeated(spent, FluidAction.EXECUTE);
        valve.consumePending(converted);
        valve.notifyContentsChanged();
        if (lastProcessedFuel != valveFuel) {
            lastProcessedFuel = valveFuel;
            setChanged();
        }
        notifySaltProduced(valveFuel);
    }

    private void notifySaltProduced(ReactorFuel valveFuel) {
        if (starterUuid == null || !(level instanceof ServerLevel serverLevel)) return;
        if (!triggeredFuels.add(valveFuel)) return;
        Player player = serverLevel.getPlayerByUUID(starterUuid);
        if (player instanceof ServerPlayer serverPlayer)
            CNCriteriaTriggers.SPENT_SALT_PRODUCED.get().trigger(serverPlayer, valveFuel.outputFluid());
    }

    private boolean hasRoomFor(ReactorFluidValveBlockEntity valve, ReactorStats.Channel channel, ReactorFuel valveFuel) {
        return valve.fillHeated(spentSalt(channel, valveFuel, 1), FluidAction.SIMULATE) > 0;
    }

    private FluidStack spentSalt(ReactorStats.Channel channel, ReactorFuel saltFuel, int amount) {
        FluidStack stack = new FluidStack(saltFuel.outputFluid().builtInRegistryHolder(), amount);
        stack.set(CNDataComponents.SALT_STATS.get(),
                stamps.computeIfAbsent(new StampKey(channel.valve(), saltFuel), key -> ReactorStats.channelComposition(channel, saltFuel)));
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
        if (starterUuid != null)
            tag.putUUID("Starter", starterUuid);
        if (lastProcessedFuel != null)
            tag.putString("LastFuel", BuiltInRegistries.FLUID.getKey(lastProcessedFuel.outputFluid()).toString());
    }

    @Nullable
    private static ReactorFuel fuelOf(String outputFluidId) {
        ResourceLocation id = ResourceLocation.tryParse(outputFluidId);
        return id == null ? null : BuiltInRegistries.FLUID.getOptional(id).map(ReactorFuel::forOutput).orElse(null);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        runTicks = tag.getLong("RunTicks");
        starterUuid = tag.hasUUID("Starter") ? tag.getUUID("Starter") : null;
        lastProcessedFuel = tag.contains("LastFuel", Tag.TAG_STRING) ? fuelOf(tag.getString("LastFuel")) : null;
    }
}
