package com.mocretion.createnuclearized.content.multiblock;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record MultiblockErrorPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<MultiblockErrorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "multiblock_error"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockErrorPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, MultiblockErrorPayload::pos, MultiblockErrorPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MultiblockErrorClient.show(payload.pos())));
    }

    public static void send(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new MultiblockErrorPayload(pos));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
