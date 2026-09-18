package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mocretion.createnuclearized.registry.CNBlocks;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class TurbineMultiblock {

    // Square cross section (width == height): only 5, 7 or 9
    public static final int MIN_SQUARE = 5;
    public static final int MAX_SQUARE = 9;
    // Depth (along the shaft) is free, just bounded so a runaway fill cant hang
    public static final int MIN_DEPTH = 5;
    public static final int MAX_DEPTH = 32;

    private static final int MAX_SHELL_VISITED = MAX_SQUARE * MAX_SQUARE * MAX_DEPTH;

    private TurbineMultiblock() {}

    public record Result(boolean valid, String reason, BlockPos min, BlockPos max,
                         Direction.Axis depthAxis, Direction inward, int square, int depth,
                         int stages, int bladeCells) {
        public static Result fail(String reason) {
            return new Result(false, reason, BlockPos.ZERO, BlockPos.ZERO,
                    Direction.Axis.X, Direction.NORTH, 0, 0, 0, 0);
        }

        public static Result ok(BlockPos min, BlockPos max, Direction.Axis depthAxis, Direction inward,
                                int square, int depth, int stages, int bladeCells) {
            return new Result(true, "", min, max, depthAxis, inward, square, depth, stages, bladeCells);
        }
    }

    private enum SliceKind { EMPTY, ROTOR, DIAPHRAGM }

    private record Slice(SliceKind kind, int side, int depthCoord, boolean shaftOnAxis) {}

    // Either a read slice, or the reason the slice at this depth is invalid
    private record SliceReadout(Slice slice, String error) {}

    // Shell predicates

    public static boolean isCasing(BlockState state) {
        return state.is(CNBlocks.TURBINE_CASING.get()) || state.is(CNBlocks.TURBINE_GLASS.get());
    }

    public static boolean isController(BlockState state) {
        return state.is(CNBlocks.TURBINE_CONTROLLER.get());
    }

    public static boolean isGenerator(BlockState state) {
        return state.is(CNBlocks.TURBINE_GENERATOR.get());
    }

    public static boolean isSteamInput(BlockState state) {
        return state.is(CNBlocks.STEAM_INPUT_VALVE.get());
    }

    public static boolean isShellMember(BlockState state) {
        return isCasing(state) || isController(state) || isGenerator(state) || isSteamInput(state);
    }

    // Interior predicates

    public static boolean isRotor(BlockState state) {
        return CNBlocks.isTurbineRotor(state);
    }

    public static boolean isRotorStructural(BlockState state) {
        return state.getBlock() instanceof TurbineRotorStructuralBlock;
    }

    public static boolean isDiaphragm(BlockState state) {
        return state.is(CNBlocks.TURBINE_DIAPHRAGM.get());
    }

    /** The center spine is Create's own shaft, not a block of ours */
    public static boolean isShaft(BlockState state) {
        return state.is(AllBlocks.SHAFT.get());
    }

    /**
     * Validate the whole turbine: the shell shape plus the interior shaft and alternating blades/diaphragm stages
     * Same idea as the reactor: flood fill the shell, take the bounding box, confirm its a hollow cuboid with a square cross section, then
     * walk the interior
     */
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
        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);

        long expectedShell = (long) dx * dy * dz - (long) (dx - 2) * (dy - 2) * (dz - 2);
        if (shell.size() != expectedShell) return Result.fail("Shell is incomplete or has extra blocks attached.");

        // Walk the surface: every cell has to be a shell member, and tally the controller / generator / steam valves
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int controllers = 0;
        int generators = 0;
        int steamValves = 0;
        BlockPos generatorPos = null;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onSurface = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    if (!onSurface) continue;
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isShellMember(state))
                        return Result.fail("Turbine wall is not solid casing at " + cursor.toShortString() + ".");
                    if (isController(state)) controllers++;
                    if (isGenerator(state)) {
                        generators++;
                        generatorPos = cursor.immutable();
                    }
                    if (isSteamInput(state)) steamValves++;
                }
            }
        }

        if (controllers != 1) return Result.fail("Turbine needs exactly one controller (found " + controllers + ").");
        if (generators != 1) return Result.fail("Turbine needs exactly one generator (found " + generators + ").");
        if (steamValves < 1) return Result.fail("Turbine needs at least one steam input valve.");

        // Generator centered on a square end wall; its axis is the shaft depth (X/Z horizontal, Y vertical)
        Orientation o = deriveOrientation(generatorPos, min, max);
        if (o == null)
            return Result.fail("The generator must sit centered on a square end wall (5x5, 7x7 or 9x9).");
        if (o.square != 5 && o.square != 7 && o.square != 9)
            return Result.fail("Turbine cross section must be a square: 5x5, 7x7 or 9x9 (got " + o.square + "x" + o.square + ").");
        if (o.depth < MIN_DEPTH || o.depth > MAX_DEPTH)
            return Result.fail("Turbine depth must be between " + MIN_DEPTH + " and " + MAX_DEPTH + " (got " + o.depth + ").");

        return scanInterior(level, cursor, o, min, max);
    }

    // The two cross section axes (everything but depth), their bounds and centers, plus the inward direction along the depth axis
    private record Orientation(Direction.Axis depthAxis, Direction inward, int depthNear, int depthFar, int depth,
                               Direction.Axis axisA, int aMin, int aMax, int cA,
                               Direction.Axis axisB, int bMin, int bMax, int cB, int square) {}

    private static Orientation deriveOrientation(BlockPos gen, BlockPos min, BlockPos max) {
        // Any axis can be the depth: X/Z make a horizontal turbine, Y a vertical one
        for (Direction.Axis depthAxis : Direction.Axis.values()) {
            Direction.Axis[] plane = otherAxes(depthAxis);
            Direction.Axis a = plane[0], b = plane[1];
            int aMin = coord(min, a), aMax = coord(max, a);
            int bMin = coord(min, b), bMax = coord(max, b);
            int dMin = coord(min, depthAxis), dMax = coord(max, depthAxis);
            int side = aMax - aMin + 1;
            if (side != bMax - bMin + 1) continue; // cross section has to be square
            int cA = midpoint(aMin, aMax), cB = midpoint(bMin, bMax);
            if (cA == Integer.MIN_VALUE || cB == Integer.MIN_VALUE) continue;
            int genD = coord(gen, depthAxis);
            if (coord(gen, a) != cA || coord(gen, b) != cB) continue; // generator centered on the cross section
            if (genD != dMin && genD != dMax) continue; // and on an end wall
            Direction.AxisDirection towards = genD == dMin ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
            Direction inward = Direction.fromAxisAndDirection(depthAxis, towards);
            return new Orientation(depthAxis, inward, dMin, dMax, dMax - dMin + 1, a, aMin, aMax, cA, b, bMin, bMax, cB, side);
        }
        return null;
    }

    private static Direction.Axis[] otherAxes(Direction.Axis depth) {
        return switch (depth) {
            case X -> new Direction.Axis[] { Direction.Axis.Y, Direction.Axis.Z };
            case Y -> new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z };
            case Z -> new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Y };
        };
    }

    private static int coord(BlockPos pos, Direction.Axis axis) {
        return axis.choose(pos.getX(), pos.getY(), pos.getZ());
    }

    // Integer center of an odd length span, or MIN_VALUE if the span is even (so it has no single center)
    private static int midpoint(int lo, int hi) {
        return (lo + hi) % 2 == 0 ? (lo + hi) / 2 : Integer.MIN_VALUE;
    }

    private static Result scanInterior(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o,
                                       BlockPos min, BlockPos max) {
        int aLo = o.aMin + 1, aHi = o.aMax - 1;
        int bLo = o.bMin + 1, bHi = o.bMax - 1;

        // Depth coords in order, walking inward from the generator wall
        List<Integer> depths = new ArrayList<>();
        if (o.inward.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            for (int d = o.depthNear + 1; d < o.depthFar; d++) depths.add(d);
        } else {
            for (int d = o.depthFar - 1; d > o.depthNear; d--) depths.add(d);
        }

        List<Slice> slices = new ArrayList<>();
        for (int d : depths) {
            SliceReadout readout = readSlice(level, cursor, o, d, aLo, aHi, bLo, bHi);
            if (readout.error() != null) return Result.fail(readout.error());
            slices.add(readout.slice());
        }

        // Grammar: skip shaft only gaps, first stage must be a rotor, each rotor is immediately followed by a same size diaphragm
        int stages = 0;
        int bladeCells = 0;
        int lastStageIndex = -1;
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            if (s.kind() == SliceKind.EMPTY) continue;
            if (s.kind() == SliceKind.DIAPHRAGM)
                return Result.fail("A diaphragm needs a rotor in front of it (towards the generator).");
            // rotor: pair it with the very next slice
            if (i + 1 >= slices.size())
                return Result.fail("The last rotor needs a diaphragm right behind it.");
            Slice next = slices.get(i + 1);
            if (next.kind() != SliceKind.DIAPHRAGM)
                return Result.fail("A rotor must be followed immediately by a diaphragm, with no gap in a stage.");
            if (next.side() != s.side())
                return Result.fail("A stage's rotor and diaphragm must be the same size.");
            stages++;
            bladeCells += s.side() * s.side() - 1;
            lastStageIndex = i + 1;
            i++;
        }

        if (stages == 0)
            return Result.fail("Build at least one stage: a rotor, then a diaphragm right behind it.");

        // The shaft has to run unbroken from the generator through the last stage
        for (int k = 0; k <= lastStageIndex; k++) {
            if (!slices.get(k).shaftOnAxis())
                return Result.fail("The center shaft must run unbroken along the turbine axis from the generator through every stage.");
        }

        return Result.ok(min, max, o.depthAxis, o.inward, o.square, o.depth, stages, bladeCells);
    }

    private static SliceReadout readSlice(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o, int d,
                                          int aLo, int aHi, int bLo, int bHi) {
        BlockState center = stateAt(level, cursor, o, d, o.cA, o.cB);

        // A rotor slice: the wheels master sits on the axis (with its built in shaft) and its structural plane fills the ring
        if (isRotor(center))
            return readRotorSlice(level, o, d, aLo, aHi, bLo, bHi, center);

        boolean shaft = isShaft(center);
        boolean shaftOnAxis = shaft && center.hasProperty(BlockStateProperties.AXIS)
                && center.getValue(BlockStateProperties.AXIS) == o.depthAxis;
        if (shaft && !shaftOnAxis)
            return new SliceReadout(null, "The center shaft must run along the turbine axis, not across it.");
        if (!shaft && !center.isAir())
            return new SliceReadout(null, "Only the drive shaft or a rotor may sit on the turbine axis.");

        // Scan the ring: only a diaphragm may sit around the shaft here
        int maxR = 0;
        int nonAir = 0;
        for (int a = aLo; a <= aHi; a++) {
            for (int b = bLo; b <= bHi; b++) {
                if (a == o.cA && b == o.cB) continue;
                BlockState s = stateAt(level, cursor, o, d, a, b);
                if (s.isAir()) continue;
                nonAir++;
                if (!isDiaphragm(s))
                    return new SliceReadout(null,
                            "Only a rotor or a diaphragm may sit around the shaft (at " + cursor.toShortString() + ").");
                maxR = Math.max(maxR, Math.max(Math.abs(a - o.cA), Math.abs(b - o.cB)));
            }
        }

        if (nonAir == 0) return new SliceReadout(new Slice(SliceKind.EMPTY, 0, d, shaftOnAxis), null);

        // Diaphragm has to fill a solid square centered on the shaft, everything past it air
        for (int a = aLo; a <= aHi; a++) {
            for (int b = bLo; b <= bHi; b++) {
                if (a == o.cA && b == o.cB) continue;
                int r = Math.max(Math.abs(a - o.cA), Math.abs(b - o.cB));
                BlockState s = stateAt(level, cursor, o, d, a, b);
                boolean inside = r <= maxR;
                if (inside && !isDiaphragm(s))
                    return new SliceReadout(null, "A diaphragm must fill a solid square around the shaft (gap at "
                            + cursor.toShortString() + ").");
                if (!inside && !s.isAir())
                    return new SliceReadout(null, "Nothing may sit outside the stage square (at "
                            + cursor.toShortString() + ").");
            }
        }

        return new SliceReadout(new Slice(SliceKind.DIAPHRAGM, 2 * maxR + 1, d, shaftOnAxis), null);
    }

    // A rotor slice is one placed rotor whose axis runs along the turbine, its structural fillers make up the rest of the wheel square
    private static SliceReadout readRotorSlice(LevelAccessor level, Orientation o, int d,
                                               int aLo, int aHi, int bLo, int bHi, BlockState center) {
        if (!center.hasProperty(BlockStateProperties.AXIS) || center.getValue(BlockStateProperties.AXIS) != o.depthAxis)
            return new SliceReadout(null, "A turbine rotor must face along the turbine axis.");
        TurbineRotorBlock rotor = (TurbineRotorBlock) center.getBlock();
        int rotorRadius = rotor.radius();
        int hitboxRadius = rotor.hitboxRadius();
        int side = 2 * rotorRadius + 1;
        if (rotorRadius > (aHi - o.cA) || rotorRadius > (bHi - o.cB))
            return new SliceReadout(null, "The rotor is too big for this turbine's cross section.");

        // The rotor is one block at the center; only its solid hitbox cells are filled, the wider blade sweep is visual so it stays clear
        BlockPos masterPos = posAt(o, d, o.cA, o.cB);
        for (int a = aLo; a <= aHi; a++) {
            for (int b = bLo; b <= bHi; b++) {
                if (a == o.cA && b == o.cB) continue;
                int r = Math.max(Math.abs(a - o.cA), Math.abs(b - o.cB));
                BlockPos cell = posAt(o, d, a, b);
                BlockState s = level.getBlockState(cell);
                if (r <= hitboxRadius) {
                    // The rotors own hitbox filler, so a foreign block cant sneak into the solid core
                    if (!isRotorStructural(s) || !TurbineRotorStructuralBlock.getMaster(level, cell, s).equals(masterPos))
                        return new SliceReadout(null, "The rotor's hitbox is obstructed (at " + cell.toShortString() + ").");
                } else if (!s.isAir()) {
                    return new SliceReadout(null, "Nothing may sit in the rotor's sweep (at " + cell.toShortString() + ").");
                }
            }
        }

        // The rotors center carries the built in shaft, so it counts as shaft on axis
        return new SliceReadout(new Slice(SliceKind.ROTOR, side, d, true), null);
    }

    // The world pos for a depth coord plus the two cross section coords
    private static BlockPos posAt(Orientation o, int d, int a, int b) {
        int x = 0, y = 0, z = 0;
        switch (o.depthAxis) {
            case X -> x = d;
            case Y -> y = d;
            case Z -> z = d;
        }
        switch (o.axisA) {
            case X -> x = a;
            case Y -> y = a;
            case Z -> z = a;
        }
        switch (o.axisB) {
            case X -> x = b;
            case Y -> y = b;
            case Z -> z = b;
        }
        return new BlockPos(x, y, z);
    }

    // Build a pos from a depth coord and the two cross section coords, each mapped onto its own axis
    private static BlockState stateAt(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o,
                                      int d, int a, int b) {
        int x = 0, y = 0, z = 0;
        switch (o.depthAxis) {
            case X -> x = d;
            case Y -> y = d;
            case Z -> z = d;
        }
        switch (o.axisA) {
            case X -> x = a;
            case Y -> y = a;
            case Z -> z = a;
        }
        switch (o.axisB) {
            case X -> x = b;
            case Y -> y = b;
            case Z -> z = b;
        }
        cursor.set(x, y, z);
        return level.getBlockState(cursor);
    }

}
