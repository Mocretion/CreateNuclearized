package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.multiblock.MultiblockState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorMonitorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int RESOLVE_INTERVAL = 20;
    private static final int GRID_INTERVAL = 10;

    @Nullable
    private BlockPos controllerCache;
    private long lastResolveTick = -RESOLVE_INTERVAL;
    private int gridTimer;
    private InteriorGrid grid = InteriorGrid.EMPTY;

    public ReactorMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void serverTick() {
        if (level == null) return;
        ReactorControllerBlockEntity controller = getController();
        boolean lit = controller != null && controller.getState() != MultiblockState.STOPPED;
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(ReactorMonitorBlock.LIT) && blockState.getValue(ReactorMonitorBlock.LIT) != lit)
            level.setBlock(worldPosition, blockState.setValue(ReactorMonitorBlock.LIT, lit), Block.UPDATE_CLIENTS);
        if (++gridTimer >= GRID_INTERVAL) {
            gridTimer = 0;
            InteriorGrid next = controller == null ? InteriorGrid.EMPTY : controller.buildInteriorGrid();
            if (!next.equals(grid)) {
                grid = next;
                level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
            }
        }
    }

    public InteriorGrid getGrid() {
        return grid;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Grid", grid.save());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readGrid(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null)
            readGrid(tag);
    }

    private void readGrid(CompoundTag tag) {
        if (tag.contains("Grid"))
            grid = InteriorGrid.load(tag.getCompound("Grid"));
    }

    @Nullable
    public ReactorControllerBlockEntity getController() {
        if (level == null) return null;
        long now = level.getGameTime();
        if (now - lastResolveTick >= RESOLVE_INTERVAL) {
            lastResolveTick = now;
            controllerCache = resolveControllerPos();
        }
        if (controllerCache != null && level.getBlockEntity(controllerCache) instanceof ReactorControllerBlockEntity controller)
            return controller;
        return null;
    }

    @Nullable
    private BlockPos resolveControllerPos() {
        BlockPos controllerPos = ReactorMultiblock.findController(level, worldPosition);
        return controllerPos != null && level.getBlockEntity(controllerPos) instanceof ReactorControllerBlockEntity ? controllerPos : null;
    }

    public int getHeatedBuffered() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getHeatedBuffered();
    }

    public int getFissileBuffered() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getFissileBuffered();
    }

    public int getFissileCapacity() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getFissileCapacity();
    }

    public int getSpentCapacity() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getSpentCapacity();
    }

    public int getRunSeconds() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getRunSeconds();
    }

    public int getReactorStateOrdinal() {
        ReactorControllerBlockEntity controller = getController();
        return (controller == null ? MultiblockState.STOPPED : controller.getState()).ordinal();
    }

    public int getActiveChannels() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getActiveChannels();
    }

    public int getTotalChannels() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getTotalChannels();
    }

    public int getTotalRateMilli() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? 0 : controller.getTotalRateMilli();
    }

    @Nullable
    public SaltStats getOutputStats() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? null : controller.getOutputStats();
    }

    @Nullable
    public ReactorFuel getFuel() {
        ReactorControllerBlockEntity controller = getController();
        return controller == null ? null : controller.getFuel();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorMonitorMenu(containerId, playerInventory, this);
    }
}
