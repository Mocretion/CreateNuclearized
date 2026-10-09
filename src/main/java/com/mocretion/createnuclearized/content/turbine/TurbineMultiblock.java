package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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

    public static final int MIN_SQUARE = 5;
    public static final int MAX_SQUARE = 9;
    public static final int MIN_DEPTH = 5;
    public static final int MAX_DEPTH = 32;

    private static final int MAX_SHELL_VISITED = MAX_SQUARE * MAX_SQUARE * MAX_DEPTH;
    private static final String SQUARE_RANGE = squareLabel(MIN_SQUARE) + " to " + squareLabel(MAX_SQUARE);

    private TurbineMultiblock() {}

    public record Result(boolean valid, String reason, Direction.Axis depthAxis, BlockPos generator,
                         List<BlockPos> steamValves, int stages, int bladeCells) {
        public static Result fail(String reason) {
            return new Result(false, reason, Direction.Axis.X, BlockPos.ZERO, List.of(), 0, 0);
        }

        public static Result ok(Direction.Axis depthAxis, BlockPos generator, List<BlockPos> steamValves,
                                int stages, int bladeCells) {
            return new Result(true, "", depthAxis, generator, List.copyOf(steamValves), stages, bladeCells);
        }
    }

    private enum SliceKind { EMPTY, ROTOR, DIAPHRAGM }

    private record Slice(SliceKind kind, int side, boolean shaftOnAxis) {}

    private record SliceReadout(Slice slice, String error) {}

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

    public static boolean isRotor(BlockState state) {
        return CNBlocks.isTurbineRotor(state);
    }

    public static boolean isRotorStructural(BlockState state) {
        return state.getBlock() instanceof TurbineRotorStructuralBlock;
    }

    public static boolean isDiaphragm(BlockState state) {
        return state.is(CNBlocks.TURBINE_DIAPHRAGM.get());
    }

    public static boolean isShaft(BlockState state) {
        return state.is(AllBlocks.SHAFT.get());
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
        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);

        long expectedShell = (long) dx * dy * dz - (long) (dx - 2) * (dy - 2) * (dz - 2);
        if (shell.size() != expectedShell) return Result.fail("Shell is incomplete or has extra blocks attached.");

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int controllers = 0;
        List<BlockPos> generators = new ArrayList<>();
        List<BlockPos> steamValves = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onSurface = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    if (!onSurface) continue;
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isShellMember(state))
                        return Result.fail("Turbine wall is not solid casing at " + cursor.toShortString() + ".");
                    if (isController(state))
                        controllers++;
                    else if (isGenerator(state))
                        generators.add(cursor.immutable());
                    else if (isSteamInput(state))
                        steamValves.add(cursor.immutable());
                }
            }
        }

        if (controllers != 1) return Result.fail("Turbine needs exactly one controller (found " + controllers + ").");
        if (generators.size() != 1) return Result.fail("Turbine needs exactly one generator (found " + generators.size() + ").");
        if (steamValves.isEmpty()) return Result.fail("Turbine needs at least one steam input valve.");

        BlockPos generator = generators.get(0);
        Orientation o = deriveOrientation(generator, min, max);
        if (o == null)
            return Result.fail("The generator must sit centered on a square end wall with an odd side (" + SQUARE_RANGE + ").");
        if (o.square < MIN_SQUARE || o.square > MAX_SQUARE)
            return Result.fail("Turbine cross section must be a square from " + SQUARE_RANGE + " (got " + squareLabel(o.square) + ").");
        if (o.depth < MIN_DEPTH || o.depth > MAX_DEPTH)
            return Result.fail("Turbine depth must be between " + MIN_DEPTH + " and " + MAX_DEPTH + " (got " + o.depth + ").");

        return scanInterior(level, cursor, o, generator, steamValves);
    }

    private static String squareLabel(int side) {
        return side + "x" + side;
    }

    private record Orientation(Direction.Axis depthAxis, Direction inward, int depthNear, int depthFar, int depth,
                               Direction.Axis axisA, int aMin, int aMax, int cA,
                               Direction.Axis axisB, int bMin, int bMax, int cB, int square) {}

    private static Orientation deriveOrientation(BlockPos gen, BlockPos min, BlockPos max) {
        for (Direction.Axis depthAxis : Direction.Axis.values()) {
            Direction.Axis[] plane = otherAxes(depthAxis);
            Direction.Axis a = plane[0], b = plane[1];
            int aMin = coord(min, a), aMax = coord(max, a);
            int bMin = coord(min, b), bMax = coord(max, b);
            int dMin = coord(min, depthAxis), dMax = coord(max, depthAxis);
            int side = aMax - aMin + 1;
            if (side != bMax - bMin + 1) continue;
            int cA = midpoint(aMin, aMax), cB = midpoint(bMin, bMax);
            if (cA == Integer.MIN_VALUE || cB == Integer.MIN_VALUE) continue;
            int genD = coord(gen, depthAxis);
            if (coord(gen, a) != cA || coord(gen, b) != cB) continue;
            if (genD != dMin && genD != dMax) continue;
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

    private static int midpoint(int lo, int hi) {
        return (lo + hi) % 2 == 0 ? (lo + hi) / 2 : Integer.MIN_VALUE;
    }

    private static Result scanInterior(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o,
                                       BlockPos generator, List<BlockPos> steamValves) {
        int aLo = o.aMin + 1, aHi = o.aMax - 1;
        int bLo = o.bMin + 1, bHi = o.bMax - 1;

        List<Integer> depths = new ArrayList<>();
        for (int d = o.depthNear + 1; d < o.depthFar; d++)
            depths.add(d);
        if (o.inward.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            Collections.reverse(depths);

        List<Slice> slices = new ArrayList<>();
        for (int d : depths) {
            SliceReadout readout = readSlice(level, cursor, o, d, aLo, aHi, bLo, bHi);
            if (readout.error() != null) return Result.fail(readout.error());
            slices.add(readout.slice());
        }

        int stages = 0;
        int bladeCells = 0;
        int lastStageIndex = -1;
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            if (s.kind() == SliceKind.EMPTY) continue;
            if (s.kind() == SliceKind.DIAPHRAGM)
                return Result.fail("A diaphragm needs a rotor in front of it (towards the generator).");
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

        for (int k = 0; k <= lastStageIndex; k++) {
            if (!slices.get(k).shaftOnAxis())
                return Result.fail("The center shaft must run unbroken along the turbine axis from the generator through every stage.");
        }

        return Result.ok(o.depthAxis, generator, steamValves, stages, bladeCells);
    }

    private static SliceReadout readSlice(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o, int d,
                                          int aLo, int aHi, int bLo, int bHi) {
        BlockState center = stateAt(level, cursor, o, d, o.cA, o.cB);

        if (isRotor(center))
            return readRotorSlice(level, o, d, aLo, aHi, bLo, bHi, center);

        boolean shaft = isShaft(center);
        boolean shaftOnAxis = shaft && center.hasProperty(BlockStateProperties.AXIS)
                && center.getValue(BlockStateProperties.AXIS) == o.depthAxis;
        if (shaft && !shaftOnAxis)
            return new SliceReadout(null, "The center shaft must run along the turbine axis, not across it.");
        if (!shaft && !center.isAir())
            return new SliceReadout(null, "Only the drive shaft or a rotor may sit on the turbine axis.");

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

        if (nonAir == 0) return new SliceReadout(new Slice(SliceKind.EMPTY, 0, shaftOnAxis), null);

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

        return new SliceReadout(new Slice(SliceKind.DIAPHRAGM, 2 * maxR + 1, shaftOnAxis), null);
    }

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

        BlockPos masterPos = posAt(o, d, o.cA, o.cB);
        for (int a = aLo; a <= aHi; a++) {
            for (int b = bLo; b <= bHi; b++) {
                if (a == o.cA && b == o.cB) continue;
                int r = Math.max(Math.abs(a - o.cA), Math.abs(b - o.cB));
                BlockPos cell = posAt(o, d, a, b);
                BlockState s = level.getBlockState(cell);
                if (r <= hitboxRadius) {
                    if (!isRotorStructural(s) || !TurbineRotorStructuralBlock.getMaster(level, cell, s).equals(masterPos))
                        return new SliceReadout(null, "The rotor's hitbox is obstructed (at " + cell.toShortString() + ").");
                } else if (!s.isAir()) {
                    return new SliceReadout(null, "Nothing may sit in the rotor's sweep (at " + cell.toShortString() + ").");
                }
            }
        }

        return new SliceReadout(new Slice(SliceKind.ROTOR, side, true), null);
    }

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

    private static BlockState stateAt(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o,
                                      int d, int a, int b) {
        cursor.set(posAt(o, d, a, b));
        return level.getBlockState(cursor);
    }

}
