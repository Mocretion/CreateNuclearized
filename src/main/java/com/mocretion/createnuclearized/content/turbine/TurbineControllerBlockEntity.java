package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineControllerBlockEntity extends BlockEntity implements MenuProvider {

    private static final int TICKS_PER_SECOND = 20;

    // Steam a stage burns per blade block each tick Provisional, the real output tuning lands with the generator
    private static final int STEAM_PER_BLADE_CELL = 2;

    // Idle turbine spins back up once the buffer holds a second of steam, or one full input valve, whichever is less
    private static final int RUN_UP_SECONDS = 1;

    private boolean formed;
    @Nullable
    private BlockPos min;
    @Nullable
    private BlockPos max;
    private Direction.Axis depthAxis = Direction.Axis.X;
    private Direction inward = Direction.NORTH;
    private int square;
    private int depth;
    private int stages;
    private int bladeCells;

    private boolean running;
    private int timer;
    private TurbineState state = TurbineState.STOPPED;
    private int steamUse; // steam drained last tick
    private boolean spinning; // running with steam vs idling to refill the buffer
    private long runTicks;

    public TurbineControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Assembly bookkeeping

    private void setFormed(TurbineMultiblock.Result r) {
        formed = true;
        min = r.min().immutable();
        max = r.max().immutable();
        depthAxis = r.depthAxis();
        inward = r.inward();
        square = r.square();
        depth = r.depth();
        stages = r.stages();
        bladeCells = r.bladeCells();
        setChanged();
    }

    private void clearFormed() {
        formed = false;
        min = null;
        max = null;
        stages = 0;
        bladeCells = 0;
        steamUse = 0;
        spinning = false;
        setChanged();
    }

    public boolean isFormed() {
        return formed;
    }

    // Telemetry

    public boolean isRunning() {
        return running;
    }

    public TurbineState getState() {
        return state;
    }

    public int getStateOrdinal() {
        return state.ordinal();
    }

    public int getStages() {
        return stages;
    }

    public int getBladeCells() {
        return bladeCells;
    }

    public int getSteamUse() {
        return steamUse;
    }

    /** Steam this turbine wants each tick when running */
    public int getSteamDemand() {
        return bladeCells * STEAM_PER_BLADE_CELL;
    }

    public int getSteamBuffered() {
        int total = 0;
        for (SteamInputValveBlockEntity valve : findSteamValves())
            total += valve.getSteamAmount();
        return total;
    }

    public int getSteamCapacity() {
        int total = 0;
        for (SteamInputValveBlockEntity valve : findSteamValves())
            total += valve.getSteamTank().getCapacity();
        return total;
    }

    public int getRunSeconds() {
        return (int) (runTicks / TICKS_PER_SECOND);
    }

    /** The generators current output speed, for the menu readout */
    public int getGeneratedRpm() {
        TurbineGeneratorBlockEntity generator = findGenerator();
        return generator != null ? Math.round(Math.abs(generator.getGeneratedSpeed())) : 0;
    }

    // Client side setters the menus data slots drive

    public void setRunningFromNetwork(boolean running) {
        this.running = running;
    }

    public void setStateFromNetwork(int ordinal) {
        this.state = TurbineState.byOrdinal(ordinal);
    }

    /** Toggle the turbine. Starting needs a currently valid structure; the player gets a message when it fails */
    public void toggleRunning(@Nullable Player player) {
        if (level == null) return;
        if (running) {
            TurbineGeneratorBlockEntity generator = findGenerator();
            if (generator != null)
                generator.stopTurbine();
            running = false;
            state = TurbineState.STOPPED;
            steamUse = 0;
            spinning = false;
            setChanged();
            return;
        }
        TurbineMultiblock.Result result = TurbineMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            clearFormed();
            if (player != null)
                player.displayClientMessage(Component.literal("Turbine invalid: " + result.reason()), true);
            return;
        }
        setFormed(result);
        runTicks = 0;
        running = true;
        spinning = false; // wait for a full run up buffer before the first spin
        TurbineGeneratorBlockEntity generator = findGenerator();
        if (generator != null)
            generator.startTurbine();
        setChanged();
    }

    // Ticking

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (++timer >= TICKS_PER_SECOND) {
            timer = 0;
            revalidate();
        }
        if (running && formed) {
            tickConsume();
        } else {
            steamUse = 0;
        }
    }

    // Guards a running turbine once a second. The rotors and shaft stay put as real blocks while it runs, so this re checks the whole
    // structure (interior included), otherwise pulling the blades out would leave it generating power for free
    private void revalidate() {
        if (!running)
            return;
        TurbineMultiblock.Result result = TurbineMultiblock.validate(level, worldPosition);
        if (result.valid())
            return;
        TurbineGeneratorBlockEntity generator = findGenerator();
        if (generator != null)
            generator.stopTurbine();
        running = false;
        clearFormed();
        state = TurbineState.STOPPED;
    }

    /**
     * Run the steam hysteresis and drive the generator
     * An idle turbine waits until the buffer holds a full run up window (RUN_UP_SECONDS) before spinning up again, and a spinning turbine
     * drops back to idle the moment the buffer runs dry. So a weak steam supply pulses: fill, run, empty, refill
     */
    private void tickConsume() {
        int demand = getSteamDemand();

        int runUp = Math.min(demand * RUN_UP_SECONDS * TICKS_PER_SECOND, SteamInputValveBlockEntity.CAPACITY);
        if (!spinning && getSteamBuffered() >= runUp)
            spinning = true;

        int drawn = 0;
        if (spinning) {
            for (SteamInputValveBlockEntity valve : findSteamValves()) {
                if (drawn >= demand) break;
                drawn += valve.consume(demand - drawn);
            }
            if (drawn <= 0)
                spinning = false;
        }

        steamUse = drawn;
        state = spinning ? TurbineState.RUNNING : TurbineState.IDLE;

        // The generator spins only while were feeding steam; idle just pauses it in place
        TurbineGeneratorBlockEntity generator = findGenerator();
        if (generator != null)
            generator.setHasSteam(spinning);

        runTicks++;
    }

    @Nullable
    private TurbineGeneratorBlockEntity findGenerator() {
        if (level == null || min == null || max == null) return null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean onSurface = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    if (!onSurface) continue;
                    cursor.set(x, y, z);
                    if (TurbineMultiblock.isGenerator(level.getBlockState(cursor))
                            && level.getBlockEntity(cursor) instanceof TurbineGeneratorBlockEntity generator)
                        return generator;
                }
            }
        }
        return null;
    }

    private List<SteamInputValveBlockEntity> findSteamValves() {
        List<SteamInputValveBlockEntity> valves = new ArrayList<>();
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
                    if (TurbineMultiblock.isSteamInput(level.getBlockState(cursor))
                            && level.getBlockEntity(cursor) instanceof SteamInputValveBlockEntity valve)
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
        return new TurbineControllerMenu(containerId, playerInventory, this);
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Running", running);
        tag.putInt("Square", square);
        tag.putInt("Depth", depth);
        tag.putInt("Stages", stages);
        tag.putInt("BladeCells", bladeCells);
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
        running = tag.getBoolean("Running");
        square = tag.getInt("Square");
        depth = tag.getInt("Depth");
        stages = tag.getInt("Stages");
        bladeCells = tag.getInt("BladeCells");
        runTicks = tag.getLong("RunTicks");
        min = tag.contains("Min") ? NbtUtils.readBlockPos(tag, "Min").orElse(null) : null;
        max = tag.contains("Max") ? NbtUtils.readBlockPos(tag, "Max").orElse(null) : null;
    }
}
