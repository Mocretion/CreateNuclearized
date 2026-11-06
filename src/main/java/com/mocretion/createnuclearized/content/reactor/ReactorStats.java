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
import net.minecraft.world.level.block.state.BlockState;

public final class ReactorStats {

    private ReactorStats() {}

    private static final Direction[] HORIZONTAL =
            { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    public record Channel(BlockPos valve, int burnShift, int purityShift, double rate, int reflectorPercent) {
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
        int heatPercent;

        void addModerator(double moderatorRate, int moderatorHeat, List<CoolerCell> coolers) {
            rate += moderatorRate;
            heatPercent += moderatorHeat;
            for (CoolerCell cell : coolers) {
                ReactorModifier.Effect cooler = cell.effect();
                burn += cooler.dBurn();
                purity += cooler.dPurity();
                rate += cooler.dRate();
                heatPercent += cooler.dHeat();
            }
        }
    }

    public enum ModRole { MODERATOR, COOLER, REFLECTOR }

    private record CoolerCell(BlockPos pos, Block block, ReactorModifier.Effect effect) {}

    public record ModKey(Block block, ModRole role) {}

    public record ModEntry(Block block, ModRole role, int count) {}

    public record ModPosKey(int col, int row, Block block, ModRole role) {}

    public record ModPosEntry(int col, int row, Block block, ModRole role, int count) {}

    public record Layout(List<Channel> channels, Map<Long, List<ModEntry>> modifiers,
            Map<Long, List<ModPosEntry>> modifierPositions, int originX, int originZ) {}

    public static Layout scan(Level level, ReactorMultiblock.Structure structure) {
        Map<Long, Shifts> byColumn = new HashMap<>();
        Map<Long, Map<ModKey, Integer>> modsByColumn = new HashMap<>();
        Map<Long, Map<ModPosKey, Integer>> modPositionsByColumn = new HashMap<>();
        for (BlockPos valve : structure.outputValves()) {
            long column = ReactorMultiblock.column(valve);
            byColumn.put(column, new Shifts());
            modsByColumn.put(column, new LinkedHashMap<>());
            modPositionsByColumn.put(column, new LinkedHashMap<>());
        }

        int originX = structure.min().getX() + 1;
        int originZ = structure.min().getZ() + 1;
        BlockPos interiorMin = structure.min().offset(1, 1, 1);
        BlockPos interiorMax = structure.max().offset(-1, -1, -1);
        for (BlockPos pos : BlockPos.betweenClosed(interiorMin, interiorMax)) {
            double moderatorRate = ReactorModerator.rate(level.getBlockState(pos));
            if (moderatorRate <= 0) continue;
            Set<Long> bridged = bridgedChannels(level, pos, byColumn.keySet());
            if (bridged.isEmpty()) continue;
            Map<Long, Integer> reflected = collinearReflectors(level, pos, byColumn.keySet());
            if (bridged.size() < 2 && reflected.isEmpty()) continue;
            List<CoolerCell> coolers = coolerCells(level, pos, byColumn.keySet());
            Block moderatorBlock = level.getBlockState(pos).getBlock();
            int moderatorHeat = ReactorModerator.heat(level.getBlockState(pos));
            int modCol = pos.getX() - originX;
            int modRow = pos.getZ() - originZ;
            for (long column : bridged) {
                byColumn.get(column).addModerator(moderatorRate, moderatorHeat, coolers);
                Map<ModKey, Integer> mods = modsByColumn.get(column);
                Map<ModPosKey, Integer> modPositions = modPositionsByColumn.get(column);
                mods.merge(new ModKey(moderatorBlock, ModRole.MODERATOR), 1, Integer::sum);
                modPositions.merge(new ModPosKey(modCol, modRow, moderatorBlock, ModRole.MODERATOR), 1, Integer::sum);
                for (CoolerCell cooler : coolers) {
                    mods.merge(new ModKey(cooler.block(), ModRole.COOLER), 1, Integer::sum);
                    modPositions.merge(new ModPosKey(cooler.pos().getX() - originX, cooler.pos().getZ() - originZ,
                            cooler.block(), ModRole.COOLER), 1, Integer::sum);
                }
            }
            addReflectorMods(level, pos, byColumn.keySet(), modsByColumn, modPositionsByColumn, originX, originZ);
            reflected.forEach((column, percent) -> byColumn.get(column).heatPercent += percent);
        }

        List<Channel> channels = new ArrayList<>();
        Map<Long, List<ModEntry>> modifiers = new HashMap<>();
        Map<Long, List<ModPosEntry>> modifierPositions = new HashMap<>();
        for (BlockPos valve : structure.outputValves()) {
            long column = ReactorMultiblock.column(valve);
            Shifts shifts = byColumn.get(column);
            channels.add(new Channel(valve, shifts.burn, shifts.purity, Math.max(0, shifts.rate), shifts.heatPercent));
            modifiers.put(column, modEntries(modsByColumn.get(column)));
            modifierPositions.put(column, modPosEntries(modPositionsByColumn.get(column)));
        }
        return new Layout(channels, modifiers, modifierPositions, originX, originZ);
    }

    private static List<ModEntry> modEntries(Map<ModKey, Integer> counts) {
        List<ModEntry> entries = new ArrayList<>();
        for (Map.Entry<ModKey, Integer> entry : counts.entrySet())
            entries.add(new ModEntry(entry.getKey().block(), entry.getKey().role(), entry.getValue()));
        return entries;
    }

    private static List<ModPosEntry> modPosEntries(Map<ModPosKey, Integer> counts) {
        List<ModPosEntry> entries = new ArrayList<>();
        for (Map.Entry<ModPosKey, Integer> entry : counts.entrySet()) {
            ModPosKey key = entry.getKey();
            entries.add(new ModPosEntry(key.col(), key.row(), key.block(), key.role(), entry.getValue()));
        }
        return entries;
    }

    public static SaltStats composition(List<Channel> channels, ReactorFuel fuel) {
        if (channels.isEmpty()) return SaltStats.EMPTY;
        boolean byRate = channels.stream().anyMatch(channel -> channel.rate() > 0);
        double burn = 0;
        double purity = 0;
        double reflector = 0;
        double weights = 0;
        for (Channel channel : channels) {
            double weight = byRate ? channel.rate() : 1;
            burn += channel.burn(fuel) * weight;
            purity += channel.purity(fuel) * weight;
            reflector += channel.reflectorPercent() * weight;
            weights += weight;
        }
        return withReflector(fuel.composition((int) Math.round(burn / weights), (int) Math.round(purity / weights)),
                (int) Math.round(reflector / weights));
    }

    public static SaltStats channelComposition(Channel channel, ReactorFuel fuel) {
        return withReflector(fuel.composition(channel.burn(fuel), channel.purity(fuel)), channel.reflectorPercent());
    }

    private static SaltStats withReflector(SaltStats stats, int reflectorPercent) {
        if (reflectorPercent <= 0) return stats;
        return stats.withHeat((int) Math.round(stats.heat() * (1 + reflectorPercent / 100.0)));
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

    private static Map<Long, Integer> collinearReflectors(Level level, BlockPos moderator, Set<Long> channelColumns) {
        Map<Long, Integer> byColumn = new HashMap<>();
        for (Direction dir : HORIZONTAL) {
            BlockPos pipe = moderator.relative(dir);
            if (!isChannelPipe(level, pipe, channelColumns)) continue;
            int percent = ReactorReflector.heat(level.getBlockState(moderator.relative(dir.getOpposite())));
            if (percent > 0)
                byColumn.merge(ReactorMultiblock.column(pipe), percent, Integer::sum);
        }
        return byColumn;
    }

    private static List<CoolerCell> coolerCells(Level level, BlockPos moderator, Set<Long> channelColumns) {
        List<CoolerCell> cells = new ArrayList<>();
        for (Direction dir : HORIZONTAL) {
            BlockPos neighbor = moderator.relative(dir);
            if (isChannelPipe(level, neighbor, channelColumns)) continue;
            BlockState state = level.getBlockState(neighbor);
            ReactorModifier.Effect effect = ReactorModifier.effect(state);
            if (effect != null)
                cells.add(new CoolerCell(neighbor.immutable(), state.getBlock(), effect));
        }
        return cells;
    }

    private static void addReflectorMods(Level level, BlockPos moderator, Set<Long> channelColumns,
            Map<Long, Map<ModKey, Integer>> modsByColumn,
            Map<Long, Map<ModPosKey, Integer>> modPositionsByColumn, int originX, int originZ) {
        for (Direction dir : HORIZONTAL) {
            BlockPos pipe = moderator.relative(dir);
            if (!isChannelPipe(level, pipe, channelColumns)) continue;
            BlockPos opposite = moderator.relative(dir.getOpposite());
            if (ReactorReflector.heat(level.getBlockState(opposite)) <= 0) continue;
            long column = ReactorMultiblock.column(pipe);
            Block reflectorBlock = level.getBlockState(opposite).getBlock();
            modsByColumn.get(column).merge(new ModKey(reflectorBlock, ModRole.REFLECTOR), 1, Integer::sum);
            modPositionsByColumn.get(column).merge(
                    new ModPosKey(opposite.getX() - originX, opposite.getZ() - originZ, reflectorBlock, ModRole.REFLECTOR),
                    1, Integer::sum);
        }
    }

    private static boolean isChannelPipe(Level level, BlockPos pos, Set<Long> channelColumns) {
        return channelColumns.contains(ReactorMultiblock.column(pos))
                && ReactorMultiblock.isFluidConduit(level.getBlockState(pos));
    }
}
