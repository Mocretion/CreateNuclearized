package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.multiblock.MultiblockShell;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNTags;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorMultiblock {

    public static final int MIN_SIZE = 5;
    public static final int MAX_HORIZONTAL = 11;
    public static final int MAX_HEIGHT = 7;

    private static final int MAX_SHELL_VISITED = MAX_HORIZONTAL * MAX_HORIZONTAL * MAX_HEIGHT;

    private ReactorMultiblock() {}

    public record Structure(BlockPos min, BlockPos max, BlockPos controller,
            List<BlockPos> inputValves, List<BlockPos> outputValves, List<BlockPos> controlRods) {}

    public record Result(@Nullable Structure structure, Component reason, @Nullable BlockPos at) {
        public boolean valid() {
            return structure != null;
        }

        public static Result fail(Component reason) {
            return new Result(null, reason, null);
        }

        public static Result failAt(Component reason, BlockPos at) {
            return new Result(null, reason, at);
        }

        public static Result ok(Structure structure) {
            return new Result(structure, Component.empty(), null);
        }
    }

    public static boolean isController(BlockState state) {
        return state.is(CNBlocks.REACTOR_CONTROLLER.get());
    }

    public static boolean isInputValve(BlockState state) {
        return state.is(CNBlocks.REACTOR_SALT_INPUT_VALVE.get());
    }

    public static boolean isOutputValve(BlockState state) {
        return state.is(CNBlocks.REACTOR_SALT_OUTPUT_VALVE.get());
    }

    public static boolean isControlRod(BlockState state) {
        return state.is(CNBlocks.CONTROL_ROD.get());
    }

    public static boolean isShellMember(BlockState state) {
        return state.is(CNTags.REACTOR_PARTS_SHELL);
    }

    public static boolean isFluidConduit(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock
                || state.getBlock() instanceof EncasedPipeBlock
                || state.getBlock() instanceof SmartFluidPipeBlock
                || state.getBlock() instanceof AxisPipeBlock
                || state.getBlock() instanceof FluidValveBlock
                || state.getBlock() instanceof PumpBlock;
    }

    public static boolean isRodPassable(BlockState state) {
        return state.isAir() || isFluidConduit(state);
    }

    public static boolean isPump(BlockState state) {
        return state.getBlock() instanceof PumpBlock;
    }

    private static boolean isAllowedInterior(BlockState state) {
        return state.isAir()
                || state.is(CNTags.REACTOR_PARTS_INTERIOR)
                || ReactorComponents.isComponent(state.getBlock());
    }

    @Nullable
    public static BlockPos findController(LevelAccessor level, BlockPos seed) {
        MultiblockShell shell = MultiblockShell.scan(level, seed, ReactorMultiblock::isShellMember, MAX_SHELL_VISITED);
        if (shell == null) return null;
        BlockPos controller = null;
        for (BlockPos pos : shell.members()) {
            if (!isController(level.getBlockState(pos))) continue;
            if (controller != null) return null;
            controller = pos;
        }
        return controller;
    }

    public static Result validate(LevelAccessor level, BlockPos seed) {
        MultiblockShell shell = MultiblockShell.scan(level, seed, ReactorMultiblock::isShellMember, MAX_SHELL_VISITED);
        if (shell == null) return Result.fail(MultiblockShell.tooLarge());

        int dx = shell.sizeX();
        int dy = shell.sizeY();
        int dz = shell.sizeZ();
        if (dx < MIN_SIZE || dx > MAX_HORIZONTAL || dz < MIN_SIZE || dz > MAX_HORIZONTAL)
            return Result.fail(message("footprint", MIN_SIZE, MAX_HORIZONTAL, dx, dz));
        if (dy < MIN_SIZE || dy > MAX_HEIGHT) return Result.fail(message("height", MIN_SIZE, MAX_HEIGHT, dy));

        BlockPos min = shell.min();
        BlockPos max = shell.max();
        if (shell.wallMemberCount() != shell.expectedWallSize()) return Result.fail(MultiblockShell.incomplete());

        BlockPos controller = null;
        int controllers = 0;
        List<BlockPos> inputValves = new ArrayList<>();
        List<BlockPos> outputValves = new ArrayList<>();
        List<BlockPos> controlRods = new ArrayList<>();
        Set<Long> outputColumns = new HashSet<>();
        int maxInputLevel = Integer.MIN_VALUE;
        boolean anyInputPlumbed = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (shell.isInterior(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isShellMember(state)) return Result.failAt(message("wall", pos.toShortString()), pos.immutable());
            if (isController(state)) {
                controllers++;
                controller = pos.immutable();
            }
            if (isControlRod(state)) {
                if (pos.getY() != max.getY()) return Result.failAt(message("rod_not_top", pos.toShortString()), pos.immutable());
                controlRods.add(pos.immutable());
            }
            if (isInputValve(state)) {
                inputValves.add(pos.immutable());
                if (isPlumbed(level, pos, shell)) {
                    anyInputPlumbed = true;
                    maxInputLevel = Math.max(maxInputLevel, pos.getY());
                }
            }
            if (isOutputValve(state)) {
                if (pos.getY() != min.getY())
                    return Result.failAt(message("output_not_floor", pos.toShortString()), pos.immutable());
                outputValves.add(pos.immutable());
                outputColumns.add(column(pos));
            }
        }

        if (controllers != 1) return Result.fail(message("controllers", controllers));
        if (inputValves.isEmpty()) return Result.fail(message("no_input"));
        if (outputValves.isEmpty()) return Result.fail(message("no_output"));

        boolean hasPump = false;
        int conduitCount = 0;
        int floorY = min.getY() + 1;
        Set<Long> channelColumns = new HashSet<>();
        for (BlockPos pos : shell.interior()) {
            BlockState state = level.getBlockState(pos);
            if (!isAllowedInterior(state)) return Result.failAt(message("interior", pos.toShortString()), pos.immutable());
            if (isFluidConduit(state)) {
                if (anyInputPlumbed && pos.getY() > maxInputLevel)
                    return Result.failAt(message("pipe_above_input", pos.toShortString()), pos.immutable());
                conduitCount++;
                if (pos.getY() == floorY)
                    channelColumns.add(column(pos));
            }
            if (isPump(state))
                hasPump = true;
        }

        if (conduitCount == 0) return Result.fail(message("no_pipes"));
        if (!hasPump) return Result.fail(message("no_pump"));
        if (!anyInputPlumbed) return Result.fail(message("input_unplumbed"));

        for (long col : channelColumns) {
            if (!outputColumns.contains(col))
                return Result.failAt(message("channel_no_output", describeColumn(col)), columnAt(col, floorY));
        }
        for (long col : outputColumns) {
            if (!channelColumns.contains(col))
                return Result.failAt(message("output_no_channel", describeColumn(col)), columnAt(col, min.getY()));
        }
        for (BlockPos rod : controlRods) {
            boolean overChannel = outputColumns.contains(column(rod));
            if (overChannel && !isRodPassable(level.getBlockState(rod.below())))
                return Result.failAt(message("rod_blocked", rod.toShortString()), rod.immutable());
        }

        return Result.ok(new Structure(min, max, controller,
                List.copyOf(inputValves), List.copyOf(outputValves), List.copyOf(controlRods)));
    }

    private static Component message(String key, Object... args) {
        return Component.translatable(CreateNuclearized.MODID + ".multiblock.reactor." + key, args);
    }

    static long column(BlockPos pos) {
        return (((long) pos.getX()) << 32) | (pos.getZ() & 0xffffffffL);
    }

    private static String describeColumn(long packed) {
        return "[x=" + (int) (packed >> 32) + ", z=" + (int) packed + "]";
    }

    private static BlockPos columnAt(long packed, int y) {
        return new BlockPos((int) (packed >> 32), y, (int) packed);
    }

    private static boolean isPlumbed(LevelAccessor level, BlockPos valve, MultiblockShell shell) {
        BlockPos inner = interiorNeighbor(valve, shell);
        return inner != null && isFluidConduit(level.getBlockState(inner));
    }

    @Nullable
    private static BlockPos interiorNeighbor(BlockPos pos, MultiblockShell shell) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (shell.isInterior(neighbor)) return neighbor;
        }
        return null;
    }
}
