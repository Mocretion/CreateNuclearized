package com.mocretion.createnuclearized.content.boiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public abstract class CellMultiblockBlockEntity<T extends CellMultiblockBlockEntity<T>> extends SmartBlockEntity
        implements IMultiBlockEntityContainer.Fluid {

    public static final int MAX_WIDTH = 3;
    protected static final int CELL_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE;
    private static final FluidTank NO_TANK = new FluidTank(0);

    protected FluidTank inputTank;
    protected FluidTank outputTank;
    protected int width = 1;
    private BlockPos controller;
    private BlockPos lastKnownPos;
    private boolean updateConnectivity;
    private boolean updateCapability;

    protected CellMultiblockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract Class<T> cellClass();

    protected abstract int capacityPerBlock();

    protected abstract void refreshCapability();

    protected void clientTick() {}

    protected abstract void serverTick();

    public void updateConnectivity() {
        updateConnectivity = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition)) {
            removeController(true);
            lastKnownPos = worldPosition;
            return;
        }
        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (level.isClientSide) {
            clientTick();
            return;
        }
        if (!isController() && !cellClass().isInstance(level.getBlockEntity(controller)))
            removeController(true);
        if (updateConnectivity) {
            updateConnectivity = false;
            if (isController())
                ConnectivityHandler.formMulti(this);
        }
        serverTick();
    }

    protected int clusterBlocks() {
        return width * width;
    }

    protected void forEachCell(Consumer<BlockPos> action) {
        for (int dx = 0; dx < width; dx++)
            for (int dz = 0; dz < width; dz++)
                action.accept(worldPosition.offset(dx, 0, dz));
    }

    protected List<T> siblings() {
        List<T> cells = new ArrayList<>();
        forEachCell(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (!pos.equals(worldPosition) && cellClass().isInstance(be))
                cells.add(cellClass().cast(be));
        });
        return cells;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(width - 1, 0, width - 1);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Width", width);
        if (lastKnownPos != null)
            tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (controller != null)
            tag.put("Controller", NbtUtils.writeBlockPos(controller));
        if (updateConnectivity)
            tag.putBoolean("Uninitialized", true);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        int prevWidth = width;
        width = Math.max(1, tag.getInt("Width"));
        lastKnownPos = tag.contains("LastKnownPos") ? NBTHelper.readBlockPos(tag, "LastKnownPos") : null;
        controller = tag.contains("Controller") ? NBTHelper.readBlockPos(tag, "Controller") : null;
        updateConnectivity = tag.getBoolean("Uninitialized");
        if (isController()) {
            inputTank.setCapacity(capacityPerBlock() * clusterBlocks());
            outputTank.setCapacity(capacityPerBlock() * clusterBlocks());
        }
        if (prevWidth != width) {
            updateCapability = true;
            invalidateRenderBoundingBox();
        }
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos != null ? lastKnownPos : worldPosition;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.equals(controller);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    public void setController(BlockPos pos) {
        if (level == null || level.isClientSide) return;
        if (pos.equals(controller)) return;
        if (!pos.equals(worldPosition) && cellClass().isInstance(level.getBlockEntity(pos)) && level.getBlockEntity(pos) != this)
            transferContentsTo(cellClass().cast(level.getBlockEntity(pos)));
        controller = pos;
        refreshCapability();
        setChanged();
        sendData();
    }

    protected void transferContentsTo(T target) {
        moveFluid(inputTank, target.inputTank);
        moveFluid(outputTank, target.outputTank);
        target.setChanged();
        target.sendData();
    }

    private static void moveFluid(FluidTank from, FluidTank to) {
        if (from.isEmpty()) return;
        int filled = to.fill(from.getFluid().copy(), FluidAction.EXECUTE);
        if (filled > 0)
            from.drain(filled, FluidAction.EXECUTE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getControllerBE() {
        if (isController() || !hasLevel() || !level.isLoaded(controller)) return cellClass().cast(this);
        BlockEntity be = level.getBlockEntity(controller);
        return cellClass().isInstance(be) ? cellClass().cast(be) : cellClass().cast(this);
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level == null || level.isClientSide) return;
        updateConnectivity = true;
        if (!keepContents)
            applyTankSize(1);
        controller = null;
        width = 1;
        invalidateRenderBoundingBox();
        refreshCapability();
        BlockState s = getBlockState();
        if (s.getValue(CellSlot.PROPERTY) != CellSlot.SINGLE)
            level.setBlock(worldPosition, s.setValue(CellSlot.PROPERTY, CellSlot.SINGLE), CELL_UPDATE_FLAGS);
        setChanged();
        sendData();
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = getBlockState();
        BlockPos origin = getController();
        BlockState next = state.setValue(CellSlot.PROPERTY,
                CellSlot.of(width, worldPosition.getX() - origin.getX(), worldPosition.getZ() - origin.getZ()));
        BlockState ctrlState = level.getBlockState(origin);
        if (!isController() && ctrlState.is(state.getBlock()))
            next = copyFromController(next, ctrlState);
        if (next != state)
            level.setBlock(worldPosition, next, CELL_UPDATE_FLAGS);
        invalidateRenderBoundingBox();
        refreshCapability();
        setChanged();
    }

    protected BlockState copyFromController(BlockState state, BlockState controllerState) {
        return state;
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return 1;
    }

    @Override
    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public void setHeight(int height) {
        if (height == 0 && width > 1) {
            width = 1;
            applyTankSize(1);
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return capacityPerBlock();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        if (blocks == 1 && width > 1 && isController() && !level.isClientSide)
            distributeContents(isRemoved() ? 0 : 1);
        applyTankSize(Math.max(1, blocks));
    }

    protected void distributeContents(int keptBlocks) {
        FluidStack in = excess(inputTank, keptBlocks);
        FluidStack out = excess(outputTank, keptBlocks);
        for (T sibling : siblings()) {
            if (in.isEmpty() && out.isEmpty()) return;
            in = giveTo(sibling.inputTank, in);
            out = giveTo(sibling.outputTank, out);
            sibling.setChanged();
            sibling.sendData();
        }
    }

    private FluidStack excess(FluidTank tank, int keptBlocks) {
        int amount = tank.getFluidAmount() - keptBlocks * capacityPerBlock();
        return amount > 0 ? tank.getFluid().copyWithAmount(amount) : FluidStack.EMPTY;
    }

    private static FluidStack giveTo(FluidTank tank, FluidStack stack) {
        if (stack.isEmpty()) return stack;
        int filled = tank.fill(stack.copy(), FluidAction.EXECUTE);
        return stack.copyWithAmount(stack.getAmount() - filled);
    }

    @Override
    public IFluidTank getTank(int tank) {
        return NO_TANK;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return FluidStack.EMPTY;
    }

    protected void applyTankSize(int blocks) {
        int cap = capacityPerBlock() * blocks;
        shrinkTo(inputTank, cap);
        shrinkTo(outputTank, cap);
    }

    private static void shrinkTo(FluidTank tank, int capacity) {
        tank.setCapacity(capacity);
        int overflow = tank.getFluidAmount() - capacity;
        if (overflow > 0)
            tank.drain(overflow, FluidAction.EXECUTE);
    }
}
