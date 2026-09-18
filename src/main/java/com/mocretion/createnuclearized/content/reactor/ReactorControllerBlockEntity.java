package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider {

    // Structure re validation / status refresh interval
    private static final int TICKS_PER_SECOND = 20;

    // Composition stamped on the heated salt, from ReactorStats, refreshed on each validation
    private SaltStats outputStats = SaltStats.BASE;

    // Fuel currently in the loop, detected from the buffered salt
    private ReactorFuel fuel = ReactorFuel.URANIUM;

    private boolean formed;
    private int size;
    @Nullable
    private BlockPos min;
    @Nullable
    private BlockPos max;

    private boolean running; // whether Start has been pressed
    // Starts at the full interval so the first server tick revalidates and recomputes outputStats right away, otherwise a reactor loaded
    // mid run would stamp the placeholder BASE stats for up to a second
    private int timer = TICKS_PER_SECOND;
    private ReactorState state = ReactorState.STOPPED;

    private int flowPerTick;    // heated salt made last tick, summed over the output valves
    private int activeChannels; // output valves that actually converted last tick
    private int totalChannels;  // output valves on the shell, from the last validation
    private long runTicks;      // ticks run this phase, reset on start

    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Assembly bookkeeping

    public void setFormed(int size, BlockPos min, BlockPos max) {
        boolean changed = !this.formed || this.size != size;
        this.formed = true;
        this.size = size;
        this.min = min.immutable();
        this.max = max.immutable();
        if (changed)
            setChanged();
    }

    public void clearFormed() {
        this.formed = false;
        this.size = 0;
        this.min = null;
        this.max = null;
        this.flowPerTick = 0;
        this.activeChannels = 0;
        this.totalChannels = 0;
        setChanged();
    }

    public boolean isFormed() {
        return formed;
    }

    public int getSize() {
        return size;
    }

    // Running state

    /** Whether the reactor has been started. Drives the button, not the status */
    public boolean isRunning() {
        return running;
    }

    /** Displayed status (stopped/idle/running), refreshed server side each second */
    public ReactorState getState() {
        return state;
    }

    public int getStateOrdinal() {
        return state.ordinal();
    }

    /** Heated salt produced last tick, summed over the output valves (mB/t) */
    public int getFlowPerTick() {
        return flowPerTick;
    }

    /** Output valves (channels) that actually converted salt last tick */
    public int getActiveChannels() {
        return activeChannels;
    }

    /** Total output valves (channels) on the shell */
    public int getTotalChannels() {
        return totalChannels;
    }

    /** The composition the reactor is currently stamping on its heated salt */
    public SaltStats getOutputStats() {
        return outputStats;
    }

    /**
     * Max conversion rate across all channels (mB/tick x1000 for int sync): the flow if every channel were fed
     * getFlowPerTick is the actual flow
     */
    public int getTotalRateMilli() {
        double sum = 0;
        for (ReactorFluidValveBlockEntity valve : findValves(false)) {
            sum += valve.getRate();
        }
        return (int) Math.round(sum * 1000);
    }

    /** Molten salt currently buffered across the input valve(s), or 0 if none/unresolved */
    public int getMoltenBuffered() {
        int total = 0;
        for (ReactorFluidValveBlockEntity valve : findValves(true)) {
            total += valve.getMoltenAmount();
        }
        return total;
    }

    /** Heated salt currently buffered across the output valves, or 0 if none/unresolved */
    public int getHeatedBuffered() {
        int total = 0;
        for (ReactorFluidValveBlockEntity valve : findValves(false)) {
            total += valve.getHeatedAmount();
        }
        return total;
    }

    /** Total molten salt buffer capacity: every input valves tank plus every output valves small one */
    public int getFissileCapacity() {
        int total = 0;
        for (ReactorFluidValveBlockEntity valve : findValves(true)) {
            total += valve.getMoltenTank().getCapacity();
        }
        for (ReactorFluidValveBlockEntity valve : findValves(false)) {
            total += valve.getMoltenTank().getCapacity();
        }
        return total;
    }

    /** Total fissile (molten) salt currently buffered across input and output valves */
    public int getFissileBuffered() {
        int total = 0;
        for (ReactorFluidValveBlockEntity valve : findValves(true)) {
            total += valve.getMoltenAmount();
        }
        for (ReactorFluidValveBlockEntity valve : findValves(false)) {
            total += valve.getMoltenAmount();
        }
        return total;
    }

    /** Total depleted (heated) salt buffer capacity across the output valves */
    public int getDepletedCapacity() {
        int total = 0;
        for (ReactorFluidValveBlockEntity valve : findValves(false)) {
            total += valve.getHeatedTank().getCapacity();
        }
        return total;
    }

    /** Seconds the reactor has run this phase (frozen once stopped) */
    public int getRunSeconds() {
        return (int) (runTicks / TICKS_PER_SECOND);
    }

    /** Client side setters the menus data slots drive */
    public void setRunningFromNetwork(boolean running) {
        this.running = running;
    }

    public void setStateFromNetwork(int ordinal) {
        this.state = ReactorState.byOrdinal(ordinal);
    }

    /** Toggle the reactor. Starting needs a currently valid structure; player (if any) gets a message when it fails */
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
            clearFormed();
            if (player != null)
                player.displayClientMessage(Component.literal("Reactor invalid: " + result.reason()), true);
            return;
        }
        setFormed(result.size(), result.min(), result.max());
        recomputeStats();
        runTicks = 0;
        running = true;
        setChanged();
    }

    // Ticking

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (++timer >= TICKS_PER_SECOND) {
            timer = 0;
            revalidate();
        }
        if (running && formed && min != null && max != null) {
            tickConversion();
        } else {
            flowPerTick = 0;
            activeChannels = 0;
        }
        syncStateToBlock();
    }

    /**
     * Push the current ReactorState onto the blocks STATE property so the front panel shows the right tile
     * Writes only on a change, and only to clients (no neighbour updates)
     */
    private void syncStateToBlock() {
        if (level == null) return;
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(ReactorControllerBlock.STATE)
                && blockState.getValue(ReactorControllerBlock.STATE) != state) {
            level.setBlock(worldPosition, blockState.setValue(ReactorControllerBlock.STATE, state),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** Re validate once a second, then refresh channel count and status */
    private void revalidate() {
        ReactorMultiblock.Result result = ReactorMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            if (formed || running) {
                running = false;
                clearFormed();
            }
            state = ReactorState.STOPPED;
            return;
        }
        setFormed(result.size(), result.min(), result.max());
        totalChannels = findValves(false).size();
        recomputeStats();

        if (!running) {
            state = ReactorState.STOPPED;
            return;
        }
        state = activeChannels > 0 ? ReactorState.RUNNING : ReactorState.IDLE;
    }

    /**
     * Recompute per channel rates and the combined outputStats from the interior layout
     * Runs on each validation, so a given layout always yields the same output. Needs a resolved cube
     */
    private void recomputeStats() {
        if (level == null || min == null || max == null) return;
        fuel = detectFuel();
        outputStats = ReactorStats.compute(level, min, max, findValves(false), fuel);
    }

    /** The fuel in the loop, from the first input salt buffered in any valve. Defaults to uranium */
    private ReactorFuel detectFuel() {
        for (ReactorFluidValveBlockEntity valve : findValves(false)) {
            var fluid = valve.getMoltenTank().getFluid().getFluid();
            if (ReactorFuel.isInput(fluid)) return ReactorFuel.forInput(fluid);
        }
        for (ReactorFluidValveBlockEntity valve : findValves(true)) {
            var fluid = valve.getMoltenTank().getFluid().getFluid();
            if (ReactorFuel.isInput(fluid)) return ReactorFuel.forInput(fluid);
        }
        return ReactorFuel.URANIUM;
    }

    /**
     * Convert buffered molten salt to heated salt at each output valve on its own
     * A valves rate (mB/tick, from the moderators/coolers around its channel) accumulates fractionally, and it only converts what it
     * actually has molten salt for. Heated salt is stamped with outputStats. A channel with no moderators has rate 0 and makes nothing
     */
    private void tickConversion() {
        int flow = 0;
        int active = 0;
        for (ReactorFluidValveBlockEntity output : findValves(false)) {
            output.advanceRate();
            FluidTank moltenTank = output.getMoltenTank();
            FluidTank heatedTank = output.getHeatedTank();
            FluidStack molten = moltenTank.getFluid();
            if (!molten.isEmpty() && ReactorFuel.isInput(molten.getFluid())) {
                // Each channel converts its own input salt into the matching depleted output
                ReactorFuel valveFuel = ReactorFuel.forInput(molten.getFluid());
                int want = Math.min(output.pendingWholeMb(), moltenTank.getFluidAmount());
                FluidStack heated = new FluidStack(valveFuel.outputFluid().builtInRegistryHolder(), Math.max(1, want));
                heated.set(CNDataComponents.SALT_STATS.get(), outputStats);
                // Simulate the fill first: it returns 0 when the buffered heated salt is a different fuel or carries different SaltStats
                // (two FluidStacks only stack with matching components), so draining before checking would silently void the molten salt
                if (want > 0) {
                    int converted = heatedTank.fill(heated, FluidAction.SIMULATE);
                    if (converted > 0) {
                        heated.setAmount(converted);
                        moltenTank.drain(converted, FluidAction.EXECUTE);
                        heatedTank.fill(heated, FluidAction.EXECUTE);
                        output.consumePending(converted);
                        output.notifyContentsChanged();
                        flow += converted;
                    }
                }
                // Active means actually converting: it has a rate, molten to draw from, and the heated tank can still take its output
                // (so a full or mismatched buffer reads as idle, per the status contract)
                heated.setAmount(1);
                if (output.getRate() > 0 && heatedTank.fill(heated, FluidAction.SIMULATE) > 0)
                    active++;
            }
        }
        flowPerTick = flow;
        activeChannels = active;
        state = active > 0 ? ReactorState.RUNNING : ReactorState.IDLE;
        runTicks++;
    }

    /** The shells input valves (input=true) or output valves (input=false) */
    private List<ReactorFluidValveBlockEntity> findValves(boolean input) {
        List<ReactorFluidValveBlockEntity> valves = new ArrayList<>();
        if (level == null || min == null || max == null) return valves;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean onSurface = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    if (!onSurface) continue;
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    boolean match = input
                            ? ReactorMultiblock.isInputValve(state)
                            : ReactorMultiblock.isOutputValve(state);
                    if (match && level.getBlockEntity(cursor) instanceof ReactorFluidValveBlockEntity valve)
                        valves.add(valve);
                }
            }
        }
        return valves;
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorControllerMenu(containerId, playerInventory, this);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putInt("Size", size);
        tag.putBoolean("Running", running);
        tag.putLong("RunTicks", runTicks);
        if (min != null)
            tag.put("Min", NbtUtils.writeBlockPos(min));
        if (max != null)
            tag.put("Max", NbtUtils.writeBlockPos(max));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        size = tag.getInt("Size");
        running = tag.getBoolean("Running");
        runTicks = tag.getLong("RunTicks");
        min = tag.contains("Min") ? NbtUtils.readBlockPos(tag, "Min").orElse(null) : null;
        max = tag.contains("Max") ? NbtUtils.readBlockPos(tag, "Max").orElse(null) : null;
    }
}
