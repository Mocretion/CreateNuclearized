package com.mocretion.createnuclearized.content.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// Converts surface sand to monazite in large noise-shaped regions, so only some shores and rivers get it

public class MonaziteSurfaceFeature extends Feature<MonaziteSurfaceFeature.Config> {

    private static final long REGION_SALT = 0x6D6F6E617A6974L;
    private static final long EDGE_SALT = 0x7261676765646EL;

    public record Config(HolderSet<Biome> biomes, BlockState target, BlockState state, double regionScale,
            double regionThreshold, double edgeScale, double edgeJitter, IntProvider depth) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(Config::biomes),
                BlockState.CODEC.optionalFieldOf("target", Blocks.SAND.defaultBlockState()).forGetter(Config::target),
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                Codec.DOUBLE.optionalFieldOf("region_scale", 0.0012).forGetter(Config::regionScale),
                Codec.DOUBLE.optionalFieldOf("region_threshold", 0.72).forGetter(Config::regionThreshold),
                Codec.DOUBLE.optionalFieldOf("edge_scale", 0.03).forGetter(Config::edgeScale),
                Codec.DOUBLE.optionalFieldOf("edge_jitter", 0.08).forGetter(Config::edgeJitter),
                IntProvider.codec(1, 16).optionalFieldOf("depth", UniformInt.of(2, 3)).forGetter(Config::depth)
        ).apply(i, Config::new));
    }

    public MonaziteSurfaceFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Config config = ctx.config();
        int chunkX = ctx.origin().getX() & ~15;
        int chunkZ = ctx.origin().getZ() & ~15;

        SimplexNoise region = new SimplexNoise(new XoroshiroRandomSource(level.getSeed() ^ REGION_SALT));
        SimplexNoise edge = new SimplexNoise(new XoroshiroRandomSource(level.getSeed() ^ EDGE_SALT));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placedAny = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (!config.biomes().contains(level.getBiome(pos.set(x, surfaceY, z)))) continue;

                double qualify = region.getValue(x * config.regionScale(), z * config.regionScale())
                        + edge.getValue(x * config.edgeScale(), z * config.edgeScale()) * config.edgeJitter();
                if (qualify <= config.regionThreshold()) continue;

                int depth = config.depth().sample(random);
                for (int y = surfaceY; y > level.getMinBuildHeight() && depth > 0; y--) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).is(config.target().getBlock())) break;
                    level.setBlock(pos, config.state(), Block.UPDATE_CLIENTS);
                    depth--;
                    placedAny = true;
                }
            }
        }
        return placedAny;
    }
}
