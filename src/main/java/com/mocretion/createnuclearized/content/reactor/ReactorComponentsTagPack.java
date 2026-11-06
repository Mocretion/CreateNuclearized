package com.mocretion.createnuclearized.content.reactor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public final class ReactorComponentsTagPack implements PackResources {

    private static final String PACK_ID = CreateNuclearized.MODID + "_reactor_component_tags";
    private static final ResourceLocation MODERATOR_PATH = pathFor("moderator");
    private static final ResourceLocation COOLER_PATH = pathFor("cooler");
    private static final ResourceLocation REFLECTOR_PATH = pathFor("reflector");

    private final PackLocationInfo info;

    private ReactorComponentsTagPack(PackLocationInfo info) {
        this.info = info;
    }

    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(consumer -> consumer.accept(createPack()));
    }

    private static Pack createPack() {
        PackLocationInfo info = new PackLocationInfo(PACK_ID,
                Component.literal("Create: Nuclearized reactor component tags"),
                PackSource.BUILT_IN, Optional.empty());
        Pack.Metadata metadata = new Pack.Metadata(Component.literal("Create: Nuclearized reactor component tags"),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
        return new Pack(info, new PackSupplier(info), metadata, new PackSelectionConfig(true, Pack.Position.TOP, false));
    }

    private record PackSupplier(PackLocationInfo info) implements Pack.ResourcesSupplier {
        @Override
        public PackResources openPrimary(PackLocationInfo location) {
            return new ReactorComponentsTagPack(location);
        }

        @Override
        public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return new ReactorComponentsTagPack(location);
        }
    }

    private static ResourceLocation pathFor(String role) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID,
                "tags/block/reactor_modifier/" + role + ".json");
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA) return null;
        if (location.equals(MODERATOR_PATH)) return () -> contentFor(ReactorComponents.moderatorBlocks());
        if (location.equals(COOLER_PATH)) return () -> contentFor(ReactorComponents.coolerBlocks());
        if (location.equals(REFLECTOR_PATH)) return () -> contentFor(ReactorComponents.reflectorBlocks());
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        if (!namespace.equals(CreateNuclearized.MODID)) return;
        emit(output, path, MODERATOR_PATH, ReactorComponents::moderatorBlocks);
        emit(output, path, COOLER_PATH, ReactorComponents::coolerBlocks);
        emit(output, path, REFLECTOR_PATH, ReactorComponents::reflectorBlocks);
    }

    private static void emit(ResourceOutput output, String prefix, ResourceLocation path,
            Supplier<List<Block>> blocks) {
        if (!path.getPath().startsWith(prefix)) return;
        output.accept(path, () -> contentFor(blocks.get()));
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(CreateNuclearized.MODID) : Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return info;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    @Override
    public void close() {}

    private static InputStream contentFor(List<Block> blocks) {
        StringBuilder sb = new StringBuilder("{\"values\":[");
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(BuiltInRegistries.BLOCK.getKey(blocks.get(i))).append('"');
        }
        sb.append("]}");
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
