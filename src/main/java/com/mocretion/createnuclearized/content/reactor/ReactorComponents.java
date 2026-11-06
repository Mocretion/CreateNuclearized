package com.mocretion.createnuclearized.content.reactor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mocretion.createnuclearized.CreateNuclearized;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

public final class ReactorComponents extends SimpleJsonResourceReloadListener {

    public static final String FOLDER = "reactor_components";
    private static final String DEFAULT_RESOURCE = "/data/" + CreateNuclearized.MODID + "/" + FOLDER + "/default.json";
    private static final Gson GSON = new Gson();

    public record ParsedModerator(ResourceLocation block, double rate, int heat) {
        public static final Codec<ParsedModerator> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(ParsedModerator::block),
                Codec.DOUBLE.optionalFieldOf("rate", 0.0).forGetter(ParsedModerator::rate),
                Codec.INT.optionalFieldOf("heat", 0).forGetter(ParsedModerator::heat))
                .apply(i, ParsedModerator::new));
    }

    public record ParsedReflector(ResourceLocation block, int heat) {
        public static final Codec<ParsedReflector> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(ParsedReflector::block),
                Codec.INT.fieldOf("heat").forGetter(ParsedReflector::heat))
                .apply(i, ParsedReflector::new));
    }

    public record ParsedCooler(ResourceLocation block, int burn, int purity, double rate, int heat) {
        public static final Codec<ParsedCooler> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(ParsedCooler::block),
                Codec.INT.optionalFieldOf("burn", 0).forGetter(ParsedCooler::burn),
                Codec.INT.optionalFieldOf("purity", 0).forGetter(ParsedCooler::purity),
                Codec.DOUBLE.optionalFieldOf("rate", 0.0).forGetter(ParsedCooler::rate),
                Codec.INT.optionalFieldOf("heat", 0).forGetter(ParsedCooler::heat))
                .apply(i, ParsedCooler::new));
    }

    public record File(List<ParsedModerator> moderators, List<ParsedReflector> reflectors, List<ParsedCooler> coolers) {
        public static final Codec<File> CODEC = RecordCodecBuilder.create(i -> i.group(
                ParsedModerator.CODEC.listOf().optionalFieldOf("moderators", List.of()).forGetter(File::moderators),
                ParsedReflector.CODEC.listOf().optionalFieldOf("reflectors", List.of()).forGetter(File::reflectors),
                ParsedCooler.CODEC.listOf().optionalFieldOf("coolers", List.of()).forGetter(File::coolers))
                .apply(i, File::new));
    }

    private record Snapshot(Map<Block, Double> moderators, Map<Block, Integer> moderatorHeats, Map<Block, Integer> reflectors,
            Map<Block, ReactorModifier.Effect> coolers) {

        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), Map.of(), Map.of());

        boolean isComponent(Block block) {
            return moderators.containsKey(block) || reflectors.containsKey(block) || coolers.containsKey(block);
        }
    }

    private static final class Builder {
        final Map<Block, Double> moderators = new LinkedHashMap<>();
        final Map<Block, Integer> moderatorHeats = new LinkedHashMap<>();
        final Map<Block, Integer> reflectors = new LinkedHashMap<>();
        final Map<Block, ReactorModifier.Effect> coolers = new LinkedHashMap<>();

        Snapshot build() {
            return new Snapshot(Collections.unmodifiableMap(moderators), Collections.unmodifiableMap(moderatorHeats),
                    Collections.unmodifiableMap(reflectors), Collections.unmodifiableMap(coolers));
        }
    }

    private static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "default");
    private static final Comparator<ResourceLocation> FILE_ORDER =
            Comparator.comparing((ResourceLocation id) -> !DEFAULT_ID.equals(id)).thenComparing(Comparator.naturalOrder());

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    public ReactorComponents() {
        super(GSON, FOLDER);
    }

    public static void initDefaults() {
        Builder builder = new Builder();
        try (InputStream in = ReactorComponents.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                CreateNuclearized.LOGGER.warn("Missing default reactor_components resource {}", DEFAULT_RESOURCE);
            } else {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    applyFile(DEFAULT_ID, JsonParser.parseReader(reader), builder);
                }
            }
        } catch (IOException e) {
            CreateNuclearized.LOGGER.error("Failed to load default reactor_components", e);
        }
        snapshot = builder.build();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager rm, ProfilerFiller profiler) {
        Builder builder = new Builder();
        json.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(FILE_ORDER))
                .forEach(entry -> applyFile(entry.getKey(), entry.getValue(), builder));
        snapshot = builder.build();
    }

    private static void applyFile(ResourceLocation fileId, JsonElement element, Builder builder) {
        File file = File.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(err -> CreateNuclearized.LOGGER.error(
                        "Failed to parse reactor_components {}: {}", fileId, err))
                .orElse(null);
        if (file == null) return;

        for (ParsedModerator m : file.moderators()) {
            Block block = resolveBlock(fileId, m.block());
            if (block != null) {
                builder.moderators.put(block, m.rate());
                if (m.heat() != 0) builder.moderatorHeats.put(block, m.heat());
            }
        }
        for (ParsedReflector r : file.reflectors()) {
            Block block = resolveBlock(fileId, r.block());
            if (block != null) builder.reflectors.put(block, r.heat());
        }
        for (ParsedCooler c : file.coolers()) {
            Block block = resolveBlock(fileId, c.block());
            if (block != null)
                builder.coolers.put(block, new ReactorModifier.Effect(c.burn(), c.purity(), c.rate(), c.heat()));
        }
    }

    @Nullable
    private static Block resolveBlock(ResourceLocation fileId, ResourceLocation blockId) {
        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null)
            CreateNuclearized.LOGGER.warn("reactor_components {}: unknown block {}", fileId, blockId);
        return block;
    }

    public static double moderatorRate(Block block) {
        Double rate = snapshot.moderators().get(block);
        return rate != null ? rate : 0;
    }

    public static int moderatorHeat(Block block) {
        Integer heat = snapshot.moderatorHeats().get(block);
        return heat != null ? heat : 0;
    }

    public static int reflectorHeat(Block block) {
        Integer heat = snapshot.reflectors().get(block);
        return heat != null ? heat : 0;
    }

    @Nullable
    public static ReactorModifier.Effect coolerEffect(Block block) {
        return snapshot.coolers().get(block);
    }

    public static boolean isComponent(Block block) {
        return snapshot.isComponent(block);
    }

    public static Set<Block> allComponents() {
        Snapshot current = snapshot;
        Set<Block> all = new LinkedHashSet<>();
        all.addAll(current.moderators().keySet());
        all.addAll(current.coolers().keySet());
        all.addAll(current.reflectors().keySet());
        return all;
    }

    public static List<Block> moderatorBlocks() {
        return new ArrayList<>(snapshot.moderators().keySet());
    }

    public static List<Block> reflectorBlocks() {
        return new ArrayList<>(snapshot.reflectors().keySet());
    }

    public static List<Block> coolerBlocks() {
        return new ArrayList<>(snapshot.coolers().keySet());
    }
}
