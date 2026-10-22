package com.mocretion.createnuclearized.content.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

// Used to spread ores across a thin layer i.e. sculk blocks which are usually only 1 block thick

public class OreClumpFeature extends Feature<OreClumpFeature.Config> {

    public record Config(RuleTest target, BlockState state, IntProvider size) implements FeatureConfiguration {
        private static final int MAX_SIZE = 16;

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                RuleTest.CODEC.fieldOf("target").forGetter(Config::target),
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                IntProvider.codec(1, MAX_SIZE).fieldOf("size").forGetter(Config::size)
        ).apply(i, Config::new));
    }

    public OreClumpFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config config = ctx.config();
        BlockPos origin = ctx.origin();
        if (!canTake(level, random, config, origin)) return false;

        int size = config.size().sample(random);
        List<BlockPos> clump = new ArrayList<>(List.of(origin));
        List<BlockPos> candidates = new ArrayList<>();
        while (clump.size() < size) {
            candidates.clear();
            for (BlockPos member : clump) {
                for (BlockPos p : BlockPos.betweenClosed(member.offset(-1, -1, -1), member.offset(1, 1, 1))) {
                    if (!clump.contains(p) && !candidates.contains(p) && canTake(level, random, config, p))
                        candidates.add(p.immutable());
                }
            }
            if (candidates.isEmpty()) break;
            clump.add(candidates.get(random.nextInt(candidates.size())));
        }

        for (BlockPos pos : clump) {
            level.setBlock(pos, config.state(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private static boolean canTake(WorldGenLevel level, RandomSource random, Config config, BlockPos pos) {
        if (!config.target().test(level.getBlockState(pos), random)) return false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(p).is(config.state().getBlock())) return false;
        }
        return true;
    }
}
