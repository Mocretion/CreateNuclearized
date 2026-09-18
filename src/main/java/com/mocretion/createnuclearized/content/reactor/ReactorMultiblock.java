package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayDeque;
import java.util.HashSet;
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

    // Cap on shell blocks the flood fill will visit
    // Volume of the biggest cuboid over counts its shell, so anything valid stays well under this; a runaway fill trips it instead of
    // hanging
    private static final int MAX_SHELL_VISITED = MAX_HORIZONTAL * MAX_HORIZONTAL * MAX_HEIGHT;

    private ReactorMultiblock() {}

    public record Result(boolean valid, String reason, int size, BlockPos min, BlockPos max) {
        public static Result fail(String reason) {
            return new Result(false, reason, 0, BlockPos.ZERO, BlockPos.ZERO);
        }

        public static Result ok(int size, BlockPos min, BlockPos max) {
            return new Result(true, "", size, min, max);
        }
    }

    // Shell predicates

    public static boolean isCasing(BlockState state) {
        return state.is(CNBlocks.REACTOR_CASING.get())
                || state.is(CNBlocks.REACTOR_GLASS.get());
    }

    public static boolean isController(BlockState state) {
        return state.is(CNBlocks.REACTOR_CONTROLLER.get());
    }

    /** Molten salt input valve. The highest ones y caps how far up the plumbing may reach */
    public static boolean isInputValve(BlockState state) {
        return state.is(CNBlocks.MOLTEN_SALT_INPUT_VALVE.get());
    }

    /** Heated salt output valve. One per channel, and it has to sit in the floor */
    public static boolean isOutputValve(BlockState state) {
        return state.is(CNBlocks.HEATED_SALT_OUTPUT_VALVE.get());
    }

    public static boolean isPort(BlockState state) {
        return isInputValve(state) || isOutputValve(state);
    }

    /** Control rods, only allowed in the top face */
    public static boolean isControlRod(BlockState state) {
        return state.is(CNBlocks.CONTROL_ROD.get());
    }

    public static boolean isMonitor(BlockState state) {
        return state.is(CNBlocks.REACTOR_MONITOR.get());
    }

    /** Encased kinetics that carry rotation through the wall to the interior pump */
    public static boolean isEncasedKinetic(BlockState state) {
        return state.is(CNBlocks.REACTOR_CASING_ENCASED_SHAFT.get())
                || state.is(CNBlocks.REACTOR_CASING_ENCASED_COGWHEEL.get())
                || state.is(CNBlocks.REACTOR_ENCASED_CHAIN_DRIVE.get());
    }

    /** Anything thats allowed to sit in a wall position */
    public static boolean isShellMember(BlockState state) {
        return isCasing(state) || isController(state) || isPort(state) || isControlRod(state)
                || isMonitor(state) || isEncasedKinetic(state);
    }

    // Interior predicates

    /** Any Create pipe, the pump, or a fluid valve */
    public static boolean isFluidConduit(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock
                || state.getBlock() instanceof EncasedPipeBlock
                || state.getBlock() instanceof SmartFluidPipeBlock
                || state.getBlock() instanceof AxisPipeBlock // includes the glass fluid pipe
                || state.getBlock() instanceof FluidValveBlock
                || state.getBlock() instanceof PumpBlock;
    }

    /** At least one pump has to drive the loop */
    public static boolean isPump(BlockState state) {
        return state.getBlock() instanceof PumpBlock;
    }

    /** Any Create kinetic block: cogs, shafts, gearbox, pump, our encased variants, and so on */
    public static boolean isKinetic(BlockState state) {
        return state.getBlock() instanceof KineticBlock;
    }

    public static boolean isModerator(BlockState state) {
        return ReactorModerator.isModerator(state);
    }

    public static boolean isModifier(BlockState state) {
        return ReactorModifier.isModifier(state);
    }

    /** Whats allowed in the hollow interior. Everything else fails validation */
    private static boolean isAllowedInterior(BlockState state) {
        return state.isAir() || isFluidConduit(state) || isKinetic(state)
                || isModerator(state) || isModifier(state) || state.is(Blocks.WATER);
    }

    /**
     * Validate the reactor that the shell block at seed belongs to
     * The seed is usually the controller, but any shell block works: we flood fill out from it, take the bounding box, and check the box is
     * a proper hollow cuboid with the right ports and a clean interior
     */
    public static Result validate(LevelAccessor level, BlockPos seed) {
        // Flood fill the connected shell
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

        // Bounding box has to be a cuboid within the per axis ranges
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
        int size = dx; // just a representative dimension; the reactor is a cuboid, not a cube

        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);

        // The surface members have to be exactly the shell: full outer minus hollow inner
        // Count only positions on the box surface: an encased shaft/cog, chain drive or control rod sitting just inside the wall is a valid
        // interior block, but the flood fill still grabs it (it counts as a shell member), so it mustnt count against the shell total
        long expectedShell = (long) dx * dy * dz - (long) (dx - 2) * (dy - 2) * (dz - 2);
        long surfaceMembers = 0;
        for (BlockPos p : shell) {
            if (p.getX() == minX || p.getX() == maxX || p.getY() == minY || p.getY() == maxY
                    || p.getZ() == minZ || p.getZ() == maxZ)
                surfaceMembers++;
        }
        if (surfaceMembers != expectedShell) return Result.fail("Shell is incomplete or has extra blocks attached.");

        // Walk every surface cell, confirm its a shell member, and tally controller/valves
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int controllers = 0;
        int inputValves = 0;
        int maxInputLevel = Integer.MIN_VALUE; // top of the plumbing
        boolean anyInputPlumbed = false;
        Set<Long> outputColumns = new HashSet<>(); // (x,z) of each floor output valve
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onSurface = x == minX || x == maxX
                            || y == minY || y == maxY
                            || z == minZ || z == maxZ;
                    if (!onSurface) continue;
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isShellMember(state)) return Result.fail("Reactor wall is not solid casing at " + cursor.toShortString() + ".");
                    if (isControlRod(state) && y != maxY) {
                        return Result.fail("Control rods must sit in the top face (at " + cursor.toShortString() + ").");
                    }
                    if (isController(state))
                        controllers++;
                    if (isInputValve(state)) {
                        inputValves++;
                        maxInputLevel = Math.max(maxInputLevel, y);
                        if (isPlumbed(level, cursor.immutable(), min, max))
                            anyInputPlumbed = true;
                    }
                    if (isOutputValve(state)) {
                        if (y != minY) {
                            return Result.fail("Depleted-salt output valves must sit in the reactor floor (at "
                                    + cursor.toShortString() + ").");
                        }
                        outputColumns.add(packColumn(x, z));
                    }
                }
            }
        }

        if (controllers != 1) return Result.fail("Reactor needs exactly one controller (found " + controllers + ").");
        if (inputValves < 1) return Result.fail("Reactor needs at least one fissile-salt input valve.");
        if (outputColumns.isEmpty()) return Result.fail("Reactor needs at least one depleted-salt output valve in the floor.");

        int inputLevel = maxInputLevel; // pipes may reach up to the highest input valve

        // Scan the interior plumbing
        boolean hasPump = false;
        int conduitCount = 0;
        int floorY = minY + 1; // interior floor, just above the shell floor
        Set<Long> channelColumns = new HashSet<>(); // (x,z) of each conduit touching the floor
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isAllowedInterior(state)) {
                        return Result.fail("Reactor interior must be air, pipes, shafts, moderators or modifiers (blocked at "
                                + cursor.toShortString() + ").");
                    }
                    if (isFluidConduit(state)) {
                        if (y > inputLevel) {
                            return Result.fail("No pipes allowed above the input valve (at "
                                    + cursor.toShortString() + ").");
                        }
                        conduitCount++;
                        if (y == floorY)
                            channelColumns.add(packColumn(x, z));
                    }
                    if (isPump(state))
                        hasPump = true;
                }
            }
        }

        if (conduitCount == 0) return Result.fail("Run Create fluid pipes down through the reactor interior.");
        if (!hasPump) return Result.fail("Add a Create mechanical pump to drive the salt loop.");
        if (!anyInputPlumbed) return Result.fail("Connect a pipe to the inside of a fissile-salt input valve.");

        // Every floor touching channel needs an output valve beneath it, and vice versa
        for (long col : channelColumns) {
            if (!outputColumns.contains(col)) {
                return Result.fail("The channel at " + describeColumn(col)
                        + " needs a depleted-salt output valve in the floor directly beneath it.");
            }
        }
        for (long col : outputColumns) {
            if (!channelColumns.contains(col)) {
                return Result.fail("The depleted-salt output valve at " + describeColumn(col)
                        + " needs a channel reaching down onto it.");
            }
        }

        return Result.ok(size, min, max);
    }

    private static long packColumn(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    private static String describeColumn(long packed) {
        return "[x=" + (int) (packed >> 32) + ", z=" + (int) packed + "]";
    }

    /** True if the valves interior facing neighbour is a pipe */
    private static boolean isPlumbed(LevelAccessor level, BlockPos valve, BlockPos min, BlockPos max) {
        BlockPos inner = interiorNeighbor(valve, min, max);
        return inner != null && isFluidConduit(level.getBlockState(inner));
    }

    /**
     * The one neighbour of a face placed block that lies strictly inside the cube, or null if the block sits on an edge or corner (where
     * theres no single interior neighbour)
     */
    @Nullable
    private static BlockPos interiorNeighbor(BlockPos pos, BlockPos min, BlockPos max) {
        BlockPos found = null;
        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            boolean interior = n.getX() > min.getX() && n.getX() < max.getX()
                    && n.getY() > min.getY() && n.getY() < max.getY()
                    && n.getZ() > min.getZ() && n.getZ() < max.getZ();
            if (interior) {
                if (found != null) {
                    return null; // more than one, so not a clean face cell
                }
                found = n.immutable();
            }
        }
        return found;
    }

    /** Find the controller on the surface of the given bounds, or null if there isnt one */
    @Nullable
    public static BlockPos findController(LevelAccessor level, BlockPos min, BlockPos max) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean onSurface = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    if (!onSurface) continue;
                    cursor.set(x, y, z);
                    if (isController(level.getBlockState(cursor))) return cursor.immutable();
                }
            }
        }
        return null;
    }
}
