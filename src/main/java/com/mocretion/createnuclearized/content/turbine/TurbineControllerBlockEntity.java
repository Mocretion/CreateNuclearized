package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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

    private static final int STEAM_PER_BLADE_CELL = 2;

    private static final float CAPACITY_PER_BLADE_CELL = 2f;

    private static final int RUN_UP_SECONDS = 1;

    @Nullable
    private BlockPos generatorPos;
    private List<BlockPos> valvePositions = List.of();
    private int stages;
    private int bladeCells;

    private boolean running;
    private boolean spinning;
    private float load;
    private int validationTimer = TICKS_PER_SECOND;
    private int sampleTimer;
    private int steamDrawnThisSample;
    private int steamUse;
    private TurbineState state = TurbineState.STOPPED;

    public TurbineControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

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

    public int getOutputPercent() {
        return Math.round(load * 100);
    }

    public int getSteamBuffered() {
        int total = 0;
        for (SteamInputValveBlockEntity valve : steamValves())
            total += valve.getSteamAmount();
        return total;
    }

    public int getSteamCapacity() {
        int total = 0;
        for (SteamInputValveBlockEntity valve : steamValves())
            total += valve.getSteamTank().getCapacity();
        return total;
    }

    public int getGeneratedRpm() {
        TurbineGeneratorBlockEntity generator = generator();
        return generator != null ? Math.round(Math.abs(generator.getGeneratedSpeed())) : 0;
    }

    public void setRunningFromNetwork(boolean running) {
        this.running = running;
    }

    public void setStateFromNetwork(int ordinal) {
        this.state = TurbineState.byOrdinal(ordinal);
    }

    public void toggleRunning(@Nullable Player player) {
        if (level == null) return;
        if (running) {
            stop();
            return;
        }
        TurbineMultiblock.Result result = TurbineMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            clearStructure();
            if (player != null)
                player.displayClientMessage(Component.literal("Turbine invalid: " + result.reason()), true);
            return;
        }
        applyStructure(result);
        running = true;
        validationTimer = 0;
        sampleTimer = 0;
        setChanged();
    }

    public void serverTick() {
        if (level == null || !running) return;
        if (++validationTimer >= TICKS_PER_SECOND) {
            validationTimer = 0;
            revalidate();
            if (!running) return;
        }
        tickConsume();
        if (++sampleTimer >= TICKS_PER_SECOND) {
            sampleTimer = 0;
            sampleLoad();
        }
        driveGenerator();
    }

    private void revalidate() {
        TurbineMultiblock.Result result = TurbineMultiblock.validate(level, worldPosition);
        if (!result.valid()) {
            stop();
            clearStructure();
            return;
        }
        applyStructure(result);
    }

    private void applyStructure(TurbineMultiblock.Result result) {
        generatorPos = result.generator();
        valvePositions = result.steamValves();
        stages = result.stages();
        bladeCells = result.bladeCells();
        TurbineGeneratorBlockEntity generator = generator();
        if (generator != null)
            generator.alignTo(result.depthAxis());
        setChanged();
    }

    private void clearStructure() {
        generatorPos = null;
        valvePositions = List.of();
        stages = 0;
        bladeCells = 0;
        setChanged();
    }

    private void stop() {
        running = false;
        spinning = false;
        load = 0;
        steamDrawnThisSample = 0;
        steamUse = 0;
        state = TurbineState.STOPPED;
        driveGenerator();
        setChanged();
    }

    private int steamDemand() {
        return bladeCells * STEAM_PER_BLADE_CELL;
    }

    private void tickConsume() {
        int demand = steamDemand();
        int runUp = Math.min(demand * RUN_UP_SECONDS * TICKS_PER_SECOND, SteamInputValveBlockEntity.CAPACITY);
        if (!spinning && getSteamBuffered() >= runUp)
            spinning = true;

        int drawn = spinning ? drawSteam(demand) : 0;
        if (drawn <= 0)
            spinning = false;

        steamUse = drawn;
        steamDrawnThisSample += drawn;
        state = spinning ? TurbineState.RUNNING : TurbineState.IDLE;
    }

    private int drawSteam(int demand) {
        int drawn = 0;
        for (SteamInputValveBlockEntity valve : steamValves()) {
            if (drawn >= demand) break;
            drawn += valve.consume(demand - drawn);
        }
        return drawn;
    }

    private void sampleLoad() {
        load = Math.min(1f, steamDrawnThisSample / (float) (steamDemand() * TICKS_PER_SECOND));
        steamDrawnThisSample = 0;
        setChanged();
    }

    private void driveGenerator() {
        TurbineGeneratorBlockEntity generator = generator();
        if (generator != null)
            generator.drive(bladeCells * CAPACITY_PER_BLADE_CELL * load);
    }

    @Nullable
    private TurbineGeneratorBlockEntity generator() {
        if (level == null || generatorPos == null) return null;
        return level.getBlockEntity(generatorPos) instanceof TurbineGeneratorBlockEntity generator ? generator : null;
    }

    private List<SteamInputValveBlockEntity> steamValves() {
        List<SteamInputValveBlockEntity> valves = new ArrayList<>();
        if (level == null) return valves;
        for (BlockPos pos : valvePositions) {
            if (level.getBlockEntity(pos) instanceof SteamInputValveBlockEntity valve)
                valves.add(valve);
        }
        return valves;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TurbineControllerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Running", running);
        tag.putBoolean("Spinning", spinning);
        tag.putFloat("Load", load);
        tag.putInt("Stages", stages);
        tag.putInt("BladeCells", bladeCells);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        spinning = tag.getBoolean("Spinning");
        load = tag.getFloat("Load");
        stages = tag.getInt("Stages");
        bladeCells = tag.getInt("BladeCells");
    }
}
