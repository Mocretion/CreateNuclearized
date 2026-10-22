package com.mocretion.createnuclearized.content.turbine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.multiblock.MultiblockShell;
import com.mocretion.createnuclearized.registry.CNBlocks;
import com.mocretion.createnuclearized.registry.CNTags;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class TurbineMultiblock {

    public static final int MIN_SQUARE = 5;
    public static final int MAX_SQUARE = 9;
    public static final int MIN_DEPTH = 5;
    public static final int MAX_DEPTH = 32;

    private static final int MAX_SHELL_VISITED = MAX_SQUARE * MAX_SQUARE * MAX_DEPTH;

    private TurbineMultiblock() {}

    public record Result(boolean valid, Component reason, @Nullable BlockPos at, Direction.Axis depthAxis,
                         BlockPos generator, List<BlockPos> steamValves, int stages, int bladeCells, float rotorCapacity) {
        public static Result fail(Component reason) {
            return new Result(false, reason, null, Direction.Axis.X, BlockPos.ZERO, List.of(), 0, 0, 0);
        }

        public static Result failAt(Component reason, BlockPos at) {
            return new Result(false, reason, at, Direction.Axis.X, BlockPos.ZERO, List.of(), 0, 0, 0);
        }

        public static Result ok(Direction.Axis depthAxis, BlockPos generator, List<BlockPos> steamValves,
                                int stages, int bladeCells, float rotorCapacity) {
            return new Result(true, Component.empty(), null, depthAxis, generator, List.copyOf(steamValves),
                    stages, bladeCells, rotorCapacity);
        }
    }

    private enum SliceKind { EMPTY, ROTOR, STATOR }

    private record Slice(SliceKind kind, int side, boolean shaftOnAxis, float capacity, int depth) {}

    private record SliceReadout(@Nullable Slice slice, @Nullable Component error) {}

    public static boolean isController(BlockState state) {
        return state.is(CNBlocks.TURBINE_CONTROLLER.get());
    }

    public static boolean isGenerator(BlockState state) {
        return state.is(CNBlocks.TURBINE_GENERATOR.get());
    }

    public static boolean isSteamInput(BlockState state) {
        return state.is(CNBlocks.TURBINE_STEAM_INPUT_VALVE.get());
    }

    public static boolean isShellMember(BlockState state) {
        return state.is(CNTags.TURBINE_PARTS_SHELL);
    }

    public static boolean isRotor(BlockState state) {
        return CNBlocks.isTurbineRotor(state);
    }

    public static boolean isRotorStructural(BlockState state) {
        return state.getBlock() instanceof TurbineRotorStructuralBlock;
    }

    public static boolean isStator(BlockState state) {
        return state.is(CNBlocks.TURBINE_STATOR.get());
    }

    public static boolean isShaft(BlockState state) {
        return state.is(AllBlocks.SHAFT.get());
    }

    private static boolean isAllowedInterior(BlockState state) {
        return state.isAir() || state.is(CNTags.TURBINE_PARTS_INTERIOR);
    }

    public static Result validate(LevelAccessor level, BlockPos seed) {
        MultiblockShell shell = MultiblockShell.scan(level, seed, TurbineMultiblock::isShellMember, MAX_SHELL_VISITED, true);
        if (shell == null) return Result.fail(MultiblockShell.tooLarge());

        BlockPos min = shell.min();
        BlockPos max = shell.max();
        int minX = min.getX(), minY = min.getY(), minZ = min.getZ();
        int maxX = max.getX(), maxY = max.getY(), maxZ = max.getZ();

        if (shell.faceMemberCount() != shell.expectedFaceSize()) return Result.fail(MultiblockShell.incomplete());

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int controllers = 0;
        List<BlockPos> generators = new ArrayList<>();
        List<BlockPos> steamValves = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!shell.isFace(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (!isShellMember(state))
                        return Result.failAt(message("wall", cursor.toShortString()), cursor.immutable());
                    if (isController(state))
                        controllers++;
                    else if (isGenerator(state))
                        generators.add(cursor.immutable());
                    else if (isSteamInput(state))
                        steamValves.add(cursor.immutable());
                }
            }
        }

        if (controllers != 1) return Result.fail(message("controllers", controllers));
        if (generators.size() != 1) return Result.fail(message("generators", generators.size()));
        if (steamValves.isEmpty()) return Result.fail(message("no_steam_input"));

        BlockPos generator = generators.get(0);
        Orientation o = deriveOrientation(generator, min, max);
        if (o == null) return Result.fail(message("generator_position", squareLabel(MIN_SQUARE), squareLabel(MAX_SQUARE)));
        BlockState generatorState = level.getBlockState(generator);
        if (generatorState.getValue(RotatedPillarKineticBlock.AXIS) != o.depthAxis)
            return Result.failAt(message("generator_facing", generator.toShortString()), generator);
        if (o.square < MIN_SQUARE || o.square > MAX_SQUARE)
            return Result.fail(message("cross_section", squareLabel(MIN_SQUARE), squareLabel(MAX_SQUARE), squareLabel(o.square)));
        if (o.depth < MIN_DEPTH || o.depth > MAX_DEPTH) return Result.fail(message("depth", MIN_DEPTH, MAX_DEPTH, o.depth));

        for (BlockPos pos : shell.interior()) {
            if (!isAllowedInterior(level.getBlockState(pos)))
                return Result.failAt(message("interior", pos.toShortString()), pos.immutable());
        }

        return scanInterior(level, cursor, o, generator, steamValves);
    }

    private static String squareLabel(int side) {
        return side + "x" + side;
    }

    private static Component message(String key, Object... args) {
        return Component.translatable(CreateNuclearized.MODID + ".multiblock.turbine." + key, args);
    }

    private record Orientation(Direction.Axis depthAxis, Direction inward, int depthNear, int depthFar, int depth,
                               Direction.Axis axisA, int aMin, int aMax, int cA,
                               Direction.Axis axisB, int bMin, int bMax, int cB, int square) {}

    @Nullable
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
        float rotorCapacity = 0;
        int lastRotorIndex = -1;
        int i = 0;
        while (i < slices.size()) {
            Slice s = slices.get(i);
            if (s.kind() == SliceKind.EMPTY) {
                i++;
                continue;
            }
            if (s.kind() == SliceKind.STATOR) return Result.fail(message("stator_without_rotor"));
            int radius = (s.side() - 1) / 2;
            boolean full = false;
            lastRotorIndex = i;
            if (i + 1 < slices.size() && slices.get(i + 1).kind() == SliceKind.STATOR) {
                full = statorCoversFootprint(level, cursor, o, slices.get(i + 1).depth(), radius);
                i += 2;
            } else {
                i++;
            }
            stages++;
            bladeCells += s.side() * s.side() - 1;
            rotorCapacity += full ? s.capacity() : s.capacity() / 2f;
        }

        if (stages == 0) return Result.fail(message("no_stages"));

        for (int k = 0; k <= lastRotorIndex; k++) {
            if (!slices.get(k).shaftOnAxis()) return Result.fail(message("shaft_broken"));
        }

        return Result.ok(o.depthAxis, generator, steamValves, stages, bladeCells, rotorCapacity);
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
            return new SliceReadout(null, message("shaft_across"));
        if (!shaft && !center.isAir())
            return new SliceReadout(null, message("axis_blocked"));

        boolean hasStator = false;
        for (int a = aLo; a <= aHi; a++) {
            for (int b = bLo; b <= bHi; b++) {
                if (a == o.cA && b == o.cB) continue;
                BlockState s = stateAt(level, cursor, o, d, a, b);
                if (s.isAir()) continue;
                if (!isStator(s)) return new SliceReadout(null, message("around_shaft", cursor.toShortString()));
                hasStator = true;
            }
        }

        SliceKind kind = hasStator ? SliceKind.STATOR : SliceKind.EMPTY;
        return new SliceReadout(new Slice(kind, 0, shaftOnAxis, 0, d), null);
    }

    private static boolean statorCoversFootprint(LevelAccessor level, BlockPos.MutableBlockPos cursor, Orientation o,
                                                 int depth, int radius) {
        for (int a = o.cA - radius; a <= o.cA + radius; a++) {
            for (int b = o.cB - radius; b <= o.cB + radius; b++) {
                if (a == o.cA && b == o.cB) continue;
                if (!isStator(stateAt(level, cursor, o, depth, a, b))) return false;
            }
        }
        return true;
    }

    private static SliceReadout readRotorSlice(LevelAccessor level, Orientation o, int d,
                                               int aLo, int aHi, int bLo, int bHi, BlockState center) {
        if (!center.hasProperty(BlockStateProperties.AXIS) || center.getValue(BlockStateProperties.AXIS) != o.depthAxis)
            return new SliceReadout(null, message("rotor_axis"));
        TurbineRotorBlock rotor = (TurbineRotorBlock) center.getBlock();
        int rotorRadius = rotor.radius();
        int hitboxRadius = rotor.hitboxRadius();
        int side = 2 * rotorRadius + 1;
        if (rotorRadius > (aHi - o.cA) || rotorRadius > (bHi - o.cB))
            return new SliceReadout(null, message("rotor_too_big"));

        BlockPos masterPos = posAt(o, d, o.cA, o.cB);
        for (int a = aLo; a <= aHi; a++) {
            for (int b = bLo; b <= bHi; b++) {
                if (a == o.cA && b == o.cB) continue;
                int r = Math.max(Math.abs(a - o.cA), Math.abs(b - o.cB));
                BlockPos cell = posAt(o, d, a, b);
                BlockState s = level.getBlockState(cell);
                if (r <= hitboxRadius) {
                    if (!isRotorStructural(s) || !TurbineRotorStructuralBlock.getMaster(level, cell, s).equals(masterPos))
                        return new SliceReadout(null, message("rotor_obstructed", cell.toShortString()));
                } else if (!s.isAir()) {
                    return new SliceReadout(null, message("rotor_sweep", cell.toShortString()));
                }
            }
        }

        return new SliceReadout(new Slice(SliceKind.ROTOR, side, true, (float) rotor.stressCapacity(), d), null);
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
