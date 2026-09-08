package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorStats {

    private ReactorStats() {}

    private static final Direction[] HORIZONTAL =
            { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    private static final class Column {
        double burn;
        double purity;
        double rate = 0;

        Column(ReactorFuel fuel) {
            burn = fuel.baseBurn();
            purity = fuel.basePurity();
        }
    }

    /** Compute each channels burn/purity/rate for the fuel, push them onto the output valves, and return the combined composition */
    public static SaltStats compute(Level level, BlockPos min, BlockPos max,
            List<ReactorFluidValveBlockEntity> outputValves, ReactorFuel fuel) {
        Map<Long, Column> byColumn = new HashMap<>();
        for (ReactorFluidValveBlockEntity valve : outputValves) {
            byColumn.put(columnOf(valve.getBlockPos()), new Column(fuel));
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX() + 1; x < max.getX(); x++) {
            for (int y = min.getY() + 1; y < max.getY(); y++) {
                for (int z = min.getZ() + 1; z < max.getZ(); z++) {
                    cursor.set(x, y, z);
                    double moderatorRate = ReactorModerator.rate(level.getBlockState(cursor));
                    if (moderatorRate <= 0) continue;
                    BlockPos modPos = cursor.immutable();

                    // Channels this moderator bridges: horizontal neighbours that are a channels pipe
                    Set<Long> connected = new HashSet<>();
                    for (Direction dir : HORIZONTAL) {
                        BlockPos n = modPos.relative(dir);
                        long col = packColumn(n.getX(), n.getZ());
                        if (byColumn.containsKey(col)
                                && ReactorMultiblock.isFluidConduit(level.getBlockState(n))) {
                            connected.add(col);
                        }
                    }
                    if (connected.size() < 2) {
                        continue; // must bridge at least two distinct channels
                    }

                    // Cooler effects: every face not occupied by a channel pipe (air counts)
                    List<ReactorModifier.Effect> effects = new ArrayList<>();
                    for (Direction dir : Direction.values()) {
                        BlockPos n = modPos.relative(dir);
                        BlockState ns = level.getBlockState(n);
                        boolean channelPipe = byColumn.containsKey(packColumn(n.getX(), n.getZ()))
                                && ReactorMultiblock.isFluidConduit(ns);
                        if (channelPipe) continue;
                        ReactorModifier.Effect e = ReactorModifier.effect(ns);
                        if (e != null)
                            effects.add(e);
                    }

                    for (long col : connected) {
                        Column c = byColumn.get(col);
                        c.rate += moderatorRate;
                        for (ReactorModifier.Effect e : effects) {
                            c.burn += e.dBurn();
                            c.purity += e.dPurity();
                            c.rate += e.dRate();
                        }
                    }
                }
            }
        }

        // Push per channel results onto the valves and accumulate the combined burn/purity
        double sumBurn = 0;
        double sumPurity = 0;
        for (ReactorFluidValveBlockEntity valve : outputValves) {
            Column c = byColumn.get(columnOf(valve.getBlockPos()));
            int burn = clampPercent(c.burn);
            int purity = clampPercent(c.purity);
            valve.setStats(burn, purity, Math.max(0, c.rate));
            sumBurn += burn;
            sumPurity += purity;
        }

        int valves = outputValves.size();
        if (valves == 0) return SaltStats.EMPTY;
        int avgBurn = clampPercent(sumBurn / valves);
        int avgPurity = clampPercent(sumPurity / valves);
        return fuel.composition(avgBurn, avgPurity);
    }

    private static int clampPercent(double v) {
        return (int) Math.max(0, Math.min(100, Math.round(v)));
    }

    private static long columnOf(BlockPos pos) {
        return packColumn(pos.getX(), pos.getZ());
    }

    private static long packColumn(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }
}
