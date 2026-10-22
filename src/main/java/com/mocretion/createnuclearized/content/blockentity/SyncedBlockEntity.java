package com.mocretion.createnuclearized.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SyncedBlockEntity extends BlockEntity {

    private static final int SYNC_INTERVAL = 10;

    private boolean syncPending;
    private long nextSyncTick;

    protected SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public static <T extends BlockEntity> BlockEntityTicker<T> serverTicker(Level level) {
        if (level.isClientSide) return null;
        return (lvl, pos, state, be) -> {
            if (be instanceof SyncedBlockEntity synced)
                synced.serverTick();
        };
    }

    public void notifyContentsChanged() {
        setChanged();
        syncPending = true;
    }

    public void serverTick() {
        if (!syncPending || level.getGameTime() < nextSyncTick) return;
        syncPending = false;
        nextSyncTick = level.getGameTime() + SYNC_INTERVAL;
        sendUpdate();
    }

    protected void sendUpdate() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null)
            loadAdditional(tag, registries);
    }
}
