package com.mocretion.createnuclearized.content.fluid;

import com.mocretion.createnuclearized.content.blockentity.SyncedBlockEntity;
import com.mocretion.createnuclearized.content.reactor.SaltStats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SpentSaltBlockEntity extends SyncedBlockEntity {

    private SaltStats stats = SaltStats.EMPTY;

    public SpentSaltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SaltStats getStats() {
        return stats;
    }

    public void setStats(SaltStats stats) {
        this.stats = stats == null ? SaltStats.EMPTY : stats;
        setChanged();
        sendUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        SaltStats.CODEC.encodeStart(NbtOps.INSTANCE, stats).result().ifPresent(t -> tag.put("Stats", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stats = tag.contains("Stats")
                ? SaltStats.CODEC.parse(NbtOps.INSTANCE, tag.get("Stats")).result().orElse(SaltStats.EMPTY)
                : SaltStats.EMPTY;
    }
}
