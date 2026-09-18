package com.mocretion.createnuclearized.content.boiler;

import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class ContainedBlazeBurnerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    // Both tanks hold depleted salt; input carries heat, output comes out cooled (heat 0)
    public static final int CAPACITY = 2000;
    // Steam made per tick, the main throughput knob. Water goes 1:1 to steam, salt is spent to match its heat
    public static final int STEAM_PER_TICK = 10;
    // Hood must sit 2 or 3 blocks above the basin (so theres at least one air block between them)
    private static final int MIN_HOOD_GAP = 2;
    private static final int MAX_HOOD_GAP = 3;

    private final FluidTank inputTank = new FluidTank(CAPACITY, ContainedBlazeBurnerBlockEntity::isHotSalt);
    private final FluidTank outputTank = new FluidTank(CAPACITY, s -> ReactorFuel.isOutput(s.getFluid()));
    private final IFluidHandler fluidHandler = new BurnerFluidHandler();

    // Fractional salt owed to the output but not yet whole enough to move; not persisted
    private double saltAccumulator;

    // Whether it converted last tick, drives the blaze/flame in the renderer
    private boolean active;

    // Client only blaze head animation, mirroring Create's blaze burner: peek amount + where it looks
    private final LerpedFloat headAnimation = LerpedFloat.linear();
    private final LerpedFloat headAngle = LerpedFloat.angular();

    private long lastSyncTick = Long.MIN_VALUE;

    public ContainedBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        headAngle.startWithValue(0);
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public boolean isActive() {
        return active;
    }

    public float getHeadAnimation(float partialTicks) {
        return headAnimation.getValue(partialTicks);
    }

    public float getHeadAngle(float partialTicks) {
        return headAngle.getValue(partialTicks);
    }

    /** Client side blaze animation: always watch the player, and peek up while working (sink down when idle) */
    public void clientTick() {
        float look = ContainedBlazeBurnerClient.lookAngle(worldPosition);
        float target = Float.isNaN(look) ? 0 : look;
        target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
        headAngle.chase(target, 0.25f, Chaser.exp(5));
        headAngle.tickChaser();

        headAnimation.chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        headAnimation.tickChaser();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        FluidStack input = inputTank.getFluid();
        if (input.isEmpty() || !ReactorFuel.isOutput(input.getFluid())) {
            idle();
            return;
        }
        // Only hot salt boils water; heat comes off the SaltStats stamped by the reactor
        SaltStats stats = input.get(CNDataComponents.SALT_STATS.get());
        int heat = stats == null ? 0 : stats.heat();
        if (heat <= 0) {
            idle();
            return;
        }

        // Basin with water sits directly above, drained through its fluid capability
        BlockPos basinPos = worldPosition.above();
        IFluidHandler basin = level.getCapability(Capabilities.FluidHandler.BLOCK, basinPos, Direction.DOWN);
        if (basin == null) {
            idle();
            return;
        }
        int waterAvail = basin.drain(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), FluidAction.SIMULATE).getAmount();
        if (waterAvail <= 0) {
            idle();
            return;
        }

        // Salt not already promised to the output cooler this many mB of steam
        double unspokenSalt = inputTank.getFluidAmount() - saltAccumulator;
        int saltLimitedSteam = (int) Math.floor(unspokenSalt * heat / 100.0);

        // The hood only decides how much steam is captured, not how much is made; the rest vents to the air
        int steam = Math.min(Math.min(STEAM_PER_TICK, waterAvail), saltLimitedSteam);
        if (steam <= 0) {
            idle();
            return;
        }

        // Book the salt this steam costs, then move it in whole mB once enough has piled up
        double saltThisTick = steam * 100.0 / heat;
        saltAccumulator += saltThisTick;
        int saltWhole = (int) saltAccumulator;

        FluidStack cooled = FluidStack.EMPTY;
        if (saltWhole > 0) {
            cooled = new FluidStack(input.getFluid().builtInRegistryHolder(), saltWhole);
            cooled.set(CNDataComponents.SALT_STATS.get(), cool(stats));
            // No room to offload the cooled salt, so dont convert this tick
            if (outputTank.fill(cooled, FluidAction.SIMULATE) < saltWhole) {
                saltAccumulator -= saltThisTick;
                idle();
                return;
            }
        }

        basin.drain(new FluidStack(Fluids.WATER, steam), FluidAction.EXECUTE);
        // Capture what the hood can hold (if any); the overflow just vents away
        ExhaustHoodBlockEntity hood = findHood(basinPos);
        if (hood != null)
            hood.addSteam(steam);
        if (saltWhole > 0) {
            inputTank.drain(saltWhole, FluidAction.EXECUTE);
            outputTank.fill(cooled, FluidAction.EXECUTE);
            saltAccumulator -= saltWhole;
        }

        emitSteamParticles(basinPos, hood);
        setActive(true);
        notifyContentsChanged();
    }

    private void idle() {
        setActive(false);
    }

    private void setActive(boolean value) {
        if (active == value) return;
        active = value;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // Depleted salt that still carries heat; cooled salt (heat 0) is rejected so a loop back into the input cant clog it
    private static boolean isHotSalt(FluidStack stack) {
        if (!ReactorFuel.isOutput(stack.getFluid())) return false;
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        return stats != null && stats.heat() > 0;
    }

    /** Same salt, heat drained to 0; the element percentages ride along unchanged */
    private static SaltStats cool(SaltStats stats) {
        return new SaltStats(0, stats.uranium(), stats.plutonium(), stats.thorium(), stats.byproducts());
    }

    /** The hood 2 or 3 blocks above the basin, with only air in between so the steam can rise to it */
    private ExhaustHoodBlockEntity findHood(BlockPos basinPos) {
        if (level == null) return null;
        for (int gap = MIN_HOOD_GAP; gap <= MAX_HOOD_GAP; gap++) {
            boolean clear = true;
            for (int i = 1; i < gap; i++) {
                if (!level.getBlockState(basinPos.above(i)).isAir()) {
                    clear = false;
                    break;
                }
            }
            if (clear && level.getBlockEntity(basinPos.above(gap)) instanceof ExhaustHoodBlockEntity hood)
                return hood;
        }
        return null;
    }

    private void emitSteamParticles(BlockPos basinPos, @Nullable ExhaustHoodBlockEntity hood) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        RandomSource rand = serverLevel.random;
        double baseY = basinPos.getY() + 0.9;
        // Rise only as far as the hood catches it (its bottom), or ~3 blocks up when theres no hood
        double topY = hood != null ? hood.getBlockPos().getY() : basinPos.getY() + 3.0;
        if (topY < baseY + 0.3) topY = baseY + 0.3;
        double cx = basinPos.getX() + 0.5;
        double cz = basinPos.getZ() + 0.5;
        // One gentle puff a tick, spawned in the lower part of the column so its slow rise stays under the hood
        // Spread along the column (not one hard shove from a point) so it looks the same up close as from afar
        double y = baseY + rand.nextDouble() * (topY - baseY) * 0.6;
        double x = cx + (rand.nextDouble() - 0.5) * 0.4;
        double z = cz + (rand.nextDouble() - 0.5) * 0.4;
        serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, 0.0, 1.0, 0.0, 0.03);
    }

    private void notifyContentsChanged() {
        setChanged();
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        if (now == lastSyncTick) return;
        lastSyncTick = now;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // Pipes fill hot salt in and drain cooled salt out
    private class BurnerFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return (tank == 0 ? inputTank : outputTank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return (tank == 0 ? inputTank : outputTank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isHotSalt(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isHotSalt(resource)) return 0;
            int filled = inputTank.fill(resource, action);
            if (filled > 0 && action.execute())
                notifyContentsChanged();
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            // Cooled salt keeps its SaltStats, so match components, not just the fluid
            if (!FluidStack.isSameFluidSameComponents(resource, outputTank.getFluid())) return FluidStack.EMPTY;
            return doDrain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return doDrain(maxDrain, action);
        }

        private FluidStack doDrain(int amount, FluidAction action) {
            FluidStack drained = outputTank.drain(amount, action);
            if (!drained.isEmpty() && action.execute())
                notifyContentsChanged();
            return drained;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidHandler);
        return true;
    }

    // Client sync (plain BlockEntity, so push tank NBT and the active flag)

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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("Active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InputTank"))
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        if (tag.contains("OutputTank"))
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        active = tag.getBoolean("Active");
    }
}
