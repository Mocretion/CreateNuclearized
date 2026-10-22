package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

    public record ModEntry(Block block, int count) {}

    public record Layout(List<Channel> channels, int reflectorPercent, Map<Long, List<ModEntry>> modifiers) {}

    public static Layout scan(Level level, ReactorMultiblock.Structure structure) {
        Map<Long, Shifts> byColumn = new HashMap<>();
        Map<Long, Map<Block, Integer>> modsByColumn = new HashMap<>();
        for (BlockPos valve : structure.outputValves()) {
            long column = ReactorMultiblock.column(valve);
            byColumn.put(column, new Shifts());
            modsByColumn.put(column, new LinkedHashMap<>());
        }

        int reflectorPercent = 0;
        BlockPos interiorMin = structure.min().offset(1, 1, 1);
        BlockPos interiorMax = structure.max().offset(-1, -1, -1);
        for (BlockPos pos : BlockPos.betweenClosed(interiorMin, interiorMax)) {
            double moderatorRate = ReactorModerator.rate(level.getBlockState(pos));
            if (moderatorRate <= 0) continue;
            Set<Long> bridged = bridgedChannels(level, pos, byColumn.keySet());
            if (bridged.isEmpty()) continue;
            int reflected = collinearReflectors(level, pos, byColumn.keySet());
            if (bridged.size() < 2 && reflected <= 0) continue;
            List<ReactorModifier.Effect> coolers = coolers(level, pos, byColumn.keySet());
            Block moderatorBlock = level.getBlockState(pos).getBlock();
            List<Block> coolerBlocks = coolerBlocks(level, pos, byColumn.keySet());
            for (long column : bridged) {
                byColumn.get(column).addModerator(moderatorRate, coolers);
                Map<Block, Integer> mods = modsByColumn.get(column);
                mods.merge(moderatorBlock, 1, Integer::sum);
                for (Block cooler : coolerBlocks)
                    mods.merge(cooler, 1, Integer::sum);
            }
            addReflectorMods(level, pos, byColumn.keySet(), modsByColumn);
            reflectorPercent += reflected;
        }

        List<Channel> channels = new ArrayList<>();
        Map<Long, List<ModEntry>> modifiers = new HashMap<>();
        for (BlockPos valve : structure.outputValves()) {
            long column = ReactorMultiblock.column(valve);
            Shifts shifts = byColumn.get(column);
            channels.add(new Channel(valve, shifts.burn, shifts.purity, Math.max(0, shifts.rate)));
            modifiers.put(column, modEntries(modsByColumn.get(column)));
        }
        return new Layout(channels, reflectorPercent, modifiers);
    }

    private static List<ModEntry> modEntries(Map<Block, Integer> counts) {
        List<ModEntry> entries = new ArrayList<>();
        for (Map.Entry<Block, Integer> entry : counts.entrySet())
            entries.add(new ModEntry(entry.getKey(), entry.getValue()));
        return entries;
    }

    public static SaltStats composition(List<Channel> channels, ReactorFuel fuel, int reflectorPercent) {
        if (channels.isEmpty()) return SaltStats.EMPTY;
        boolean byRate = channels.stream().anyMatch(channel -> channel.rate() > 0);
        double burn = 0;
        double purity = 0;
        double weights = 0;
        for (Channel channel : channels) {
            double weight = byRate ? channel.rate() : 1;
            burn += channel.burn(fuel) * weight;
            purity += channel.purity(fuel) * weight;
            weights += weight;
        }
        SaltStats stats = fuel.composition((int) Math.round(burn / weights), (int) Math.round(purity / weights));
        if (reflectorPercent > 0)
            stats = stats.withHeat((int) Math.round(stats.heat() * (1 + reflectorPercent / 100.0)));
        return stats;
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

    private static int collinearReflectors(Level level, BlockPos moderator, Set<Long> channelColumns) {
        int total = 0;
        for (Direction dir : HORIZONTAL) {
            if (!isChannelPipe(level, moderator.relative(dir), channelColumns)) continue;
            total += ReactorReflector.percent(level.getBlockState(moderator.relative(dir.getOpposite())));
        }
        return total;
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

    private static List<Block> coolerBlocks(Level level, BlockPos moderator, Set<Long> channelColumns) {
        List<Block> blocks = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = moderator.relative(dir);
            if (isChannelPipe(level, neighbor, channelColumns)) continue;
            BlockState state = level.getBlockState(neighbor);
            if (state.isAir() || state.is(Blocks.WATER)) continue;
            if (ReactorModifier.effect(state) != null)
                blocks.add(state.getBlock());
        }
        return blocks;
    }

    private static void addReflectorMods(Level level, BlockPos moderator, Set<Long> channelColumns,
            Map<Long, Map<Block, Integer>> modsByColumn) {
        for (Direction dir : HORIZONTAL) {
            BlockPos pipe = moderator.relative(dir);
            if (!isChannelPipe(level, pipe, channelColumns)) continue;
            BlockPos opposite = moderator.relative(dir.getOpposite());
            if (ReactorReflector.percent(level.getBlockState(opposite)) <= 0) continue;
            modsByColumn.get(ReactorMultiblock.column(pipe))
                    .merge(level.getBlockState(opposite).getBlock(), 1, Integer::sum);
        }
    }

    private static boolean isChannelPipe(Level level, BlockPos pos, Set<Long> channelColumns) {
        return channelColumns.contains(ReactorMultiblock.column(pos))
                && ReactorMultiblock.isFluidConduit(level.getBlockState(pos));
    }
}
