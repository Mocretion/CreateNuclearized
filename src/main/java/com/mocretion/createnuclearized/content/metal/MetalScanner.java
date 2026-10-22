package com.mocretion.createnuclearized.content.metal;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mocretion.createnuclearized.CreateNuclearized;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModFileInfo;

final class MetalScanner {

    static final String FOLDER = "createnuclearized/metals";
    private static final Pattern SLUG = Pattern.compile("[a-z0-9_]+");

    private MetalScanner() {}

    static Path packFolder() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER);
    }

    static List<Path> packSources() {
        Path folder = packFolder();
        List<Path> sources = new ArrayList<>();
        try {
            Files.createDirectories(folder);
            try (Stream<Path> children = Files.list(folder)) {
                children.filter(MetalScanner::isPackSource).sorted().forEach(sources::add);
            }
        } catch (IOException e) {
            CreateNuclearized.LOGGER.error("Could not read metal pack folder {}", folder, e);
        }
        return sources;
    }

    static List<MetalDefinition> scan(List<Path> packSources) {
        Map<ResourceLocation, MetalDefinition> found = new LinkedHashMap<>();
        for (IModFileInfo modFile : ModList.get().getModFiles())
            scanDataRoot(modFile.getFile().findResource("data"), modFile.getFile().getFileName(), found);
        for (Path source : packSources)
            scanPackSource(source, found);
        return new ArrayList<>(found.values());
    }

    private static boolean isPackSource(Path path) {
        String name = path.getFileName().toString();
        return Files.isDirectory(path) || name.endsWith(".zip");
    }

    private static void scanPackSource(Path source, Map<ResourceLocation, MetalDefinition> found) {
        String label = source.getFileName().toString();
        if (Files.isDirectory(source)) {
            scanDataRoot(source.resolve("data"), label, found);
            return;
        }
        try (FileSystem zip = FileSystems.newFileSystem(source)) {
            scanDataRoot(zip.getPath("data"), label, found);
        } catch (IOException e) {
            CreateNuclearized.LOGGER.error("Could not open metal pack {}", label, e);
        }
    }

    private static void scanDataRoot(Path data, String label, Map<ResourceLocation, MetalDefinition> found) {
        if (!Files.isDirectory(data)) return;
        try (Stream<Path> namespaces = Files.list(data)) {
            for (Path namespace : (Iterable<Path>) namespaces::iterator) {
                Path folder = namespace.resolve(FOLDER);
                if (Files.isDirectory(folder))
                    scanFolder(folder, fileName(namespace), label, found);
            }
        } catch (IOException e) {
            CreateNuclearized.LOGGER.error("Could not scan {} for metal definitions", label, e);
        }
    }

    private static void scanFolder(Path folder, String namespace, String label, Map<ResourceLocation, MetalDefinition> found)
            throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files.sorted()::iterator) {
                String name = fileName(file);
                if (!name.endsWith(".json")) continue;
                parse(file, namespace, name.substring(0, name.length() - ".json".length()), label, found);
            }
        }
    }

    private static void parse(Path file, String namespace, String slug, String label, Map<ResourceLocation, MetalDefinition> found) {
        ResourceLocation id = SLUG.matcher(slug).matches() ? ResourceLocation.tryBuild(namespace, slug) : null;
        if (id == null) {
            CreateNuclearized.LOGGER.error("Skipping metal {}/{}: file name must be lowercase letters, digits and underscores", label, slug);
            return;
        }
        if (found.containsKey(id)) {
            CreateNuclearized.LOGGER.warn("Skipping duplicate metal {} from {}", id, label);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            MetalDefinition.BODY_CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> CreateNuclearized.LOGGER.error("Skipping metal {} from {}: {}", id, label, error))
                    .ifPresent(body -> found.put(id, body.withId(id)));
        } catch (Exception e) {
            CreateNuclearized.LOGGER.error("Skipping metal {} from {}", id, label, e);
        }
    }

    private static String fileName(Path path) {
        return path.getFileName().toString().replace("/", "");
    }
}
