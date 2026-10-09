package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class ReactorStats {

    private ReactorStats() {}

    private static final Direction[] HORIZONTAL =
            { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    public record Channel(BlockPos valve, int burnShift, int purityShift, double rate) {
        public int burn(ReactorFuel fuel) {
            return Mth.clamp(fuel.baseBurn() + burnShift, 0, 100);
        }

        public int purity(ReactorFuel fuel) {
            return Mth.clamp(fuel.basePurity() + purityShift, 0, 100);
        }
    }

    private static final class Shifts {
        int burn;
        int purity;
        double rate;

        void addModerator(double moderatorRate, List<ReactorModifier.Effect> coolers) {
            rate += moderatorRate;
            for (ReactorModifier.Effect cooler : coolers) {
                burn += cooler.dBurn();
                purity += cooler.dPurity();
                rate += cooler.dRate();
            }
        }
    }

    public static List<Channel> channels(Level level, ReactorMultiblock.Structure structure) {
        Map<Long, Shifts> byColumn = new HashMap<>();
        for (BlockPos valve : structure.outputValves())
            byColumn.put(ReactorMultiblock.column(valve), new Shifts());

        BlockPos interiorMin = structure.min().offset(1, 1, 1);
        BlockPos interiorMax = structure.max().offset(-1, -1, -1);
        for (BlockPos pos : BlockPos.betweenClosed(interiorMin, interiorMax)) {
            double moderatorRate = ReactorModerator.rate(level.getBlockState(pos));
            if (moderatorRate <= 0) continue;
            Set<Long> bridged = bridgedChannels(level, pos, byColumn.keySet());
            if (bridged.size() < 2) continue;
            List<ReactorModifier.Effect> coolers = coolers(level, pos, byColumn.keySet());
            for (long column : bridged)
                byColumn.get(column).addModerator(moderatorRate, coolers);
        }

        List<Channel> channels = new ArrayList<>();
        for (BlockPos valve : structure.outputValves()) {
            Shifts shifts = byColumn.get(ReactorMultiblock.column(valve));
            channels.add(new Channel(valve, shifts.burn, shifts.purity, Math.max(0, shifts.rate)));
        }
        return channels;
    }

    public static SaltStats composition(List<Channel> channels, ReactorFuel fuel) {
        if (channels.isEmpty()) return SaltStats.EMPTY;
        double burn = 0;
        double purity = 0;
        for (Channel channel : channels) {
            burn += channel.burn(fuel);
            purity += channel.purity(fuel);
        }
        return fuel.composition((int) Math.round(burn / channels.size()), (int) Math.round(purity / channels.size()));
    }

    private static Set<Long> bridgedChannels(Level level, BlockPos moderator, Set<Long> channelColumns) {
        Set<Long> bridged = new HashSet<>();
        for (Direction dir : HORIZONTAL) {
            BlockPos neighbor = moderator.relative(dir);
            if (isChannelPipe(level, neighbor, channelColumns))
                bridged.add(ReactorMultiblock.column(neighbor));
        }
        return bridged;
    }

    private static List<ReactorModifier.Effect> coolers(Level level, BlockPos moderator, Set<Long> channelColumns) {
        List<ReactorModifier.Effect> coolers = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = moderator.relative(dir);
            if (isChannelPipe(level, neighbor, channelColumns)) continue;
            ReactorModifier.Effect effect = ReactorModifier.effect(level.getBlockState(neighbor));
            if (effect != null)
                coolers.add(effect);
        }
        return coolers;
    }

    private static boolean isChannelPipe(Level level, BlockPos pos, Set<Long> channelColumns) {
        return channelColumns.contains(ReactorMultiblock.column(pos))
                && ReactorMultiblock.isFluidConduit(level.getBlockState(pos));
    }
}
