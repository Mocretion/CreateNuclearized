package com.mocretion.createnuclearized.content.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public record MultiblockShell(Set<BlockPos> members, BlockPos min, BlockPos max) {

    private static final int[][] EDGE_NEIGHBORS = {
            { 1, 1, 0 }, { 1, -1, 0 }, { -1, 1, 0 }, { -1, -1, 0 },
            { 1, 0, 1 }, { 1, 0, -1 }, { -1, 0, 1 }, { -1, 0, -1 },
            { 0, 1, 1 }, { 0, 1, -1 }, { 0, -1, 1 }, { 0, -1, -1 },
    };

    @Nullable
    public static MultiblockShell scan(LevelAccessor level, BlockPos seed, Predicate<BlockState> isMember, int maxVisited) {
        return scan(level, seed, isMember, maxVisited, false);
    }

    @Nullable
    public static MultiblockShell scan(LevelAccessor level, BlockPos seed, Predicate<BlockState> isMember,
                                       int maxVisited, boolean connectAcrossEdges) {
        Set<BlockPos> members = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        members.add(seed.immutable());
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

            for (BlockPos next : neighbors(pos, connectAcrossEdges)) {
                if (members.contains(next) || !isMember.test(level.getBlockState(next))) continue;
                if (members.size() >= maxVisited) return null;
                members.add(next);
                queue.add(next);
            }
        }
        return new MultiblockShell(members, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    private static List<BlockPos> neighbors(BlockPos pos, boolean includeEdges) {
        List<BlockPos> result = new ArrayList<>(includeEdges ? 18 : 6);
        for (Direction dir : Direction.values())
            result.add(pos.relative(dir));
        if (includeEdges)
            for (int[] o : EDGE_NEIGHBORS)
                result.add(pos.offset(o[0], o[1], o[2]));
        return result;
    }

    public int sizeX() {
        return max.getX() - min.getX() + 1;
    }

    public int sizeY() {
        return max.getY() - min.getY() + 1;
    }

    public int sizeZ() {
        return max.getZ() - min.getZ() + 1;
    }

    public long expectedWallSize() {
        long dx = sizeX(), dy = sizeY(), dz = sizeZ();
        return dx * dy * dz - (dx - 2) * (dy - 2) * (dz - 2);
    }

    public long expectedFaceSize() {
        long dx = sizeX(), dy = sizeY(), dz = sizeZ();
        return 2 * ((dx - 2) * (dy - 2) + (dy - 2) * (dz - 2) + (dx - 2) * (dz - 2));
    }

    public boolean isInterior(BlockPos pos) {
        return pos.getX() > min.getX() && pos.getX() < max.getX()
                && pos.getY() > min.getY() && pos.getY() < max.getY()
                && pos.getZ() > min.getZ() && pos.getZ() < max.getZ();
    }

    private int extremeCount(BlockPos pos) {
        int extremes = 0;
        if (pos.getX() == min.getX() || pos.getX() == max.getX()) extremes++;
        if (pos.getY() == min.getY() || pos.getY() == max.getY()) extremes++;
        if (pos.getZ() == min.getZ() || pos.getZ() == max.getZ()) extremes++;
        return extremes;
    }

    public boolean isEdge(BlockPos pos) {
        return extremeCount(pos) >= 2;
    }

    public boolean isFace(BlockPos pos) {
        return extremeCount(pos) == 1;
    }

    public long wallMemberCount() {
        return members.stream().filter(pos -> !isInterior(pos)).count();
    }

    public long faceMemberCount() {
        return members.stream().filter(this::isFace).count();
    }

    public static Component tooLarge() {
        return Component.translatable(CreateNuclearized.MODID + ".multiblock.too_large");
    }

    public static Component incomplete() {
        return Component.translatable(CreateNuclearized.MODID + ".multiblock.shell_incomplete");
    }

    public Iterable<BlockPos> interior() {
        return BlockPos.betweenClosed(min.offset(1, 1, 1), max.offset(-1, -1, -1));
    }
}
