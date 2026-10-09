package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.registry.CNBlocks;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorMultiblock {

    public static final int MIN_SIZE = 5;
    public static final int MAX_HORIZONTAL = 11;
    public static final int MAX_HEIGHT = 7;

    private static final int MAX_SHELL_VISITED = MAX_HORIZONTAL * MAX_HORIZONTAL * MAX_HEIGHT;

    private ReactorMultiblock() {}

    public record Structure(BlockPos min, BlockPos max, BlockPos controller,
            List<BlockPos> inputValves, List<BlockPos> outputValves, List<BlockPos> controlRods) {}

    public record Result(@Nullable Structure structure, String reason) {
        public boolean valid() {
            return structure != null;
        }

        public static Result fail(String reason) {
            return new Result(null, reason);
        }

        public static Result ok(Structure structure) {
            return new Result(structure, "");
        }
    }

    public static boolean isCasing(BlockState state) {
        return state.is(CNBlocks.REACTOR_CASING.get())
                || state.is(CNBlocks.REACTOR_GLASS.get());
    }

    public static boolean isController(BlockState state) {
        return state.is(CNBlocks.REACTOR_CONTROLLER.get());
    }

    public static boolean isInputValve(BlockState state) {
        return state.is(CNBlocks.MOLTEN_SALT_INPUT_VALVE.get());
    }

    public static boolean isOutputValve(BlockState state) {
        return state.is(CNBlocks.HEATED_SALT_OUTPUT_VALVE.get());
    }

    public static boolean isPort(BlockState state) {
        return isInputValve(state) || isOutputValve(state);
    }

    public static boolean isControlRod(BlockState state) {
        return state.is(CNBlocks.CONTROL_ROD.get());
    }

    public static boolean isMonitor(BlockState state) {
        return state.is(CNBlocks.REACTOR_MONITOR.get());
    }

    public static boolean isEncasedKinetic(BlockState state) {
        return state.is(CNBlocks.REACTOR_CASING_ENCASED_SHAFT.get())
                || state.is(CNBlocks.REACTOR_CASING_ENCASED_COGWHEEL.get())
                || state.is(CNBlocks.REACTOR_CASING_ENCASED_LARGE_COGWHEEL.get())
                || state.is(CNBlocks.REACTOR_ENCASED_CHAIN_DRIVE.get());
    }

    public static boolean isShellMember(BlockState state) {
        return isCasing(state) || isController(state) || isPort(state) || isControlRod(state)
                || isMonitor(state) || isEncasedKinetic(state);
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

    public static boolean isKinetic(BlockState state) {
        return state.getBlock() instanceof KineticBlock;
    }

    public static boolean isModerator(BlockState state) {
        return ReactorModerator.isModerator(state);
    }

    public static boolean isModifier(BlockState state) {
        return ReactorModifier.isModifier(state);
    }

    private static boolean isAllowedInterior(BlockState state) {
        return state.isAir() || isFluidConduit(state) || isKinetic(state)
                || isModerator(state) || isModifier(state) || state.is(Blocks.WATER);
    }

    public static Result validate(LevelAccessor level, BlockPos seed) {
        Set<BlockPos> shell = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        shell.add(seed.immutable());
        queue.add(seed.immutable());

        int minX = seed.getX(), minY = seed.getY(), minZ = seed.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (shell.contains(next)) continue;
                if (isShellMember(level.getBlockState(next))) {
                    if (shell.size() >= MAX_SHELL_VISITED) return Result.fail("Structure is too large or malformed.");
                    shell.add(next);
                    queue.add(next);
                }
            }
        }

        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        if (dx < MIN_SIZE || dx > MAX_HORIZONTAL || dz < MIN_SIZE || dz > MAX_HORIZONTAL) {
            return Result.fail("Reactor width and depth must be between " + MIN_SIZE + " and "
                    + MAX_HORIZONTAL + " (got " + dx + "x" + dz + ").");
        }
        if (dy < MIN_SIZE || dy > MAX_HEIGHT) {
            return Result.fail("Reactor height must be between " + MIN_SIZE + " and " + MAX_HEIGHT
                    + " (got " + dy + ").");
        }

        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);

        long expectedShell = (long) dx * dy * dz - (long) (dx - 2) * (dy - 2) * (dz - 2);
        long wallMembers = shell.stream().filter(pos -> !isInterior(pos, min, max)).count();
        if (wallMembers != expectedShell) return Result.fail("Shell is incomplete or has extra blocks attached.");

        BlockPos controller = null;
        int controllers = 0;
        List<BlockPos> inputValves = new ArrayList<>();
        List<BlockPos> outputValves = new ArrayList<>();
        List<BlockPos> controlRods = new ArrayList<>();
        Set<Long> outputColumns = new HashSet<>();
        int maxInputLevel = Integer.MIN_VALUE;
        boolean anyInputPlumbed = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isInterior(pos, min, max)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isShellMember(state)) return Result.fail("Reactor wall is not solid casing at " + pos.toShortString() + ".");
            if (isController(state)) {
                controllers++;
                controller = pos.immutable();
            }
            if (isControlRod(state)) {
                if (pos.getY() != maxY) return Result.fail("Control rods must sit in the top face (at " + pos.toShortString() + ").");
                controlRods.add(pos.immutable());
            }
            if (isInputValve(state)) {
                inputValves.add(pos.immutable());
                maxInputLevel = Math.max(maxInputLevel, pos.getY());
                if (isPlumbed(level, pos, min, max))
                    anyInputPlumbed = true;
            }
            if (isOutputValve(state)) {
                if (pos.getY() != minY) {
                    return Result.fail("Spent-salt output valves must sit in the reactor floor (at "
                            + pos.toShortString() + ").");
                }
                outputValves.add(pos.immutable());
                outputColumns.add(column(pos));
            }
        }

        if (controllers != 1) return Result.fail("Reactor needs exactly one controller (found " + controllers + ").");
        if (inputValves.isEmpty()) return Result.fail("Reactor needs at least one fissile-salt input valve.");
        if (outputValves.isEmpty()) return Result.fail("Reactor needs at least one spent-salt output valve in the floor.");

        boolean hasPump = false;
        int conduitCount = 0;
        int floorY = minY + 1;
        Set<Long> channelColumns = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(min.offset(1, 1, 1), max.offset(-1, -1, -1))) {
            BlockState state = level.getBlockState(pos);
            if (!isAllowedInterior(state)) {
                return Result.fail("Reactor interior must be air, pipes, shafts, moderators or modifiers (blocked at "
                        + pos.toShortString() + ").");
            }
            if (isFluidConduit(state)) {
                if (pos.getY() > maxInputLevel) {
                    return Result.fail("No pipes allowed above the input valve (at "
                            + pos.toShortString() + ").");
                }
                conduitCount++;
                if (pos.getY() == floorY)
                    channelColumns.add(column(pos));
            }
            if (isPump(state))
                hasPump = true;
        }

        if (conduitCount == 0) return Result.fail("Run Create fluid pipes down through the reactor interior.");
        if (!hasPump) return Result.fail("Add a Create mechanical pump to drive the salt loop.");
        if (!anyInputPlumbed) return Result.fail("Connect a pipe to the inside of a fissile-salt input valve.");

        for (long col : channelColumns) {
            if (!outputColumns.contains(col)) {
                return Result.fail("The channel at " + describeColumn(col)
                        + " needs a spent-salt output valve in the floor directly beneath it.");
            }
        }
        for (long col : outputColumns) {
            if (!channelColumns.contains(col)) {
                return Result.fail("The spent-salt output valve at " + describeColumn(col)
                        + " needs a channel reaching down onto it.");
            }
        }
        for (BlockPos rod : controlRods) {
            boolean overChannel = outputColumns.contains(column(rod));
            if (overChannel && !isRodPassable(level.getBlockState(rod.below()))) {
                return Result.fail("The control rod at " + rod.toShortString()
                        + " needs air or a pipe directly beneath it.");
            }
        }

        return Result.ok(new Structure(min, max, controller,
                List.copyOf(inputValves), List.copyOf(outputValves), List.copyOf(controlRods)));
    }

    static long column(BlockPos pos) {
        return (((long) pos.getX()) << 32) | (pos.getZ() & 0xffffffffL);
    }

    private static String describeColumn(long packed) {
        return "[x=" + (int) (packed >> 32) + ", z=" + (int) packed + "]";
    }

    private static boolean isInterior(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() > min.getX() && pos.getX() < max.getX()
                && pos.getY() > min.getY() && pos.getY() < max.getY()
                && pos.getZ() > min.getZ() && pos.getZ() < max.getZ();
    }

    private static boolean isPlumbed(LevelAccessor level, BlockPos valve, BlockPos min, BlockPos max) {
        BlockPos inner = interiorNeighbor(valve, min, max);
        return inner != null && isFluidConduit(level.getBlockState(inner));
    }

    @Nullable
    private static BlockPos interiorNeighbor(BlockPos pos, BlockPos min, BlockPos max) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (isInterior(neighbor, min, max)) return neighbor;
        }
        return null;
    }
}
