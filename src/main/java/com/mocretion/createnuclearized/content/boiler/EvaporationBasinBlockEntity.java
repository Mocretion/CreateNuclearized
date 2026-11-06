package com.mocretion.createnuclearized.content.boiler;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.advancement.CNCriteriaTriggers;
import com.mocretion.createnuclearized.content.recipe.CachedRecipes;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.content.reactor.SaltStatsTooltip;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class EvaporationBasinBlockEntity extends CellMultiblockBlockEntity<EvaporationBasinBlockEntity>
        implements IHaveGoggleInformation {

    public static final int CAPACITY = 100;
    private static final int MAX_HOOD_GAP = 8;
    private static final int EFFECT_INTERVAL = 10;
    private static final int EMIT_TIMEOUT = 3;
    private static final int DIVERT_TIMEOUT = 5;
    private static final double BUDGET_EPSILON = 1e-6;

    private SmartFluidTankBehaviour inputBehaviour;
    private SmartFluidTankBehaviour outputBehaviour;
    private IFluidHandler fluidHandler;
    private SharedFilteringBehaviour filtering;
    private BasinValueBox valueBox;

    private boolean blocked;

    private double degasBudget;
    private FluidStack degasBatch = FluidStack.EMPTY;
    private double gasCarry;
    private int effectCooldown;

    private int emittingTimer;
    private int divertTimer;
    private int beamCrossingY;
    private AABB beamBounds;
    private FluidStack carriedGas = FluidStack.EMPTY;
    private boolean emitting;
    private ParticleOptions beamParticle;
    private int lastSyncedColor = -1;

    public EvaporationBasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputBehaviour = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, CAPACITY, false)
                .forbidExtraction();
        outputBehaviour = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, CAPACITY, false)
                .forbidInsertion();
        inputTank = inputBehaviour.getPrimaryHandler().setValidator(this::accepts);
        outputTank = outputBehaviour.getPrimaryHandler();
        valueBox = new BasinValueBox();
        filtering = new SharedFilteringBehaviour(this, valueBox);
        filtering.forRecipes();
        behaviours.add(filtering);
        behaviours.add(inputBehaviour);
        behaviours.add(outputBehaviour);
        refreshCapability();
    }

    @Override
    protected Class<EvaporationBasinBlockEntity> cellClass() {
        return EvaporationBasinBlockEntity.class;
    }

    @Override
    protected int capacityPerBlock() {
        return CAPACITY;
    }

    @Override
    protected void refreshCapability() {
        EvaporationBasinBlockEntity ctrl = getControllerBE();
        fluidHandler = new CombinedTankWrapper(ctrl.inputBehaviour.getCapability(), ctrl.outputBehaviour.getCapability());
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    private FluidTank input() {
        return getControllerBE().inputTank;
    }

    private FluidTank output() {
        return getControllerBE().outputTank;
    }

    @Override
    protected void serverTick() {
        releaseStaleFilter();
        if (effectCooldown > 0)
            effectCooldown--;
        if (emittingTimer > 0)
            emittingTimer--;
        if (divertTimer > 0)
            divertTimer--;
        FluidStack in = input().getFluid().copy();
        boolean boiled = !in.isEmpty() && tryBoil(in);
        DegassingRecipe degassing = boiled ? null : findDegassing(degasBudget > 0 ? degasBatch : in);
        if (degassing != null)
            degas(degassing, in);
        else {
            if (!boiled)
                degasBudget = 0;
            setBlocked(false);
        }
        syncEmitting();
    }

    private void syncEmitting() {
        boolean now = emittingTimer > 0;
        int color = getBeamColor();
        if (now == emitting && color == lastSyncedColor) return;
        emitting = now;
        lastSyncedColor = color;
        notifyUpdate();
    }

    private boolean tryBoil(FluidStack in) {
        if (!(level.getBlockEntity(worldPosition.below()) instanceof IndustrialHeaterBlockEntity cell)) return false;
        IndustrialHeaterBlockEntity burner = cell.getControllerBE();
        BoilingRecipe boiling = findBoiling(in, burner);
        return boiling != null && boil(boiling, burner, in);
    }

    private boolean boil(BoilingRecipe recipe, IndustrialHeaterBlockEntity burner, FluidStack in) {
        int maxWater = Math.min(recipe.maxRate(), input().getFluidAmount());
        if (maxWater <= 0) return false;
        int amount = burner.boil(maxWater, recipe);
        if (amount <= 0) return false;

        input().drain(amount, FluidAction.EXECUTE);
        releaseGas(gasStack(recipe.gas(), amount), recipe.gas(), in, recipe.particles(), recipe.hazards());
        return true;
    }

    private void degas(DegassingRecipe recipe, FluidStack in) {
        boolean needsInput = degasBudget < recipe.rate() - BUDGET_EPSILON;
        FluidStack gas = needsInput && recipe.accepts(in) ? payBatch(recipe, in) : FluidStack.EMPTY;
        if (degasBudget <= 0) return;
        degasBudget -= recipe.rate();
        if (degasBudget < BUDGET_EPSILON)
            degasBudget = 0;
        releaseGas(gas, recipe.gas(), degasBatch, recipe.particles(), recipe.hazards());
    }

    private FluidStack payBatch(DegassingRecipe recipe, FluidStack in) {
        int amount = Math.min(Mth.ceil(recipe.rate() - degasBudget), in.getAmount());
        double gasExact = amount * recipe.gasShare(in) + gasCarry;
        int gas = (int) gasExact;
        FluidStack rest = recipe.outputLiquid(in, amount, gas);
        if (!rest.isEmpty() && output().fill(rest, FluidAction.SIMULATE) < rest.getAmount()) {
            setBlocked(true);
            return FluidStack.EMPTY;
        }
        setBlocked(false);

        gasCarry = gasExact - gas;
        input().drain(amount, FluidAction.EXECUTE);
        if (!rest.isEmpty())
            output().fill(rest, FluidAction.EXECUTE);
        degasBudget += amount;
        degasBatch = in.copyWithAmount(1);
        return gasStack(recipe.gas(), gas);
    }

    @Nullable
    private BoilingRecipe findBoiling(FluidStack in, IndustrialHeaterBlockEntity burner) {
        for (RecipeHolder<BoilingRecipe> holder : CachedRecipes.of(level, CNRecipeTypes.BOILING.get())) {
            BoilingRecipe recipe = holder.value();
            if (recipe.input().test(in) && filterAccepts(recipe.gas()) && burner.hasHeatFor(recipe)) return recipe;
        }
        return null;
    }

    @Nullable
    private DegassingRecipe findDegassing(FluidStack in) {
        for (RecipeHolder<DegassingRecipe> holder : CachedRecipes.of(level, CNRecipeTypes.DEGASSING.get()))
            if (holder.value().accepts(in) && filterAccepts(holder.value().gas())) return holder.value();
        return null;
    }

    private boolean filterAccepts(Optional<net.minecraft.world.level.material.Fluid> gas) {
        FilteringBehaviour filter = getControllerBE().filtering;
        return filter == null || gas.isEmpty() || filter.test(new FluidStack(gas.get().builtInRegistryHolder(), 1));
    }

    private static FluidStack gasStack(Optional<net.minecraft.world.level.material.Fluid> gas, int amount) {
        if (gas.isEmpty() || amount <= 0) return FluidStack.EMPTY;
        return new FluidStack(gas.get().builtInRegistryHolder(), amount);
    }

    private boolean accepts(FluidStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<BoilingRecipe> holder : CachedRecipes.of(level, CNRecipeTypes.BOILING.get()))
            if (holder.value().input().test(stack)) return true;
        return findDegassing(stack) != null;
    }

    private void releaseGas(FluidStack gas, Optional<net.minecraft.world.level.material.Fluid> recipeGas, FluidStack input,
            GasParticles particles, GasHazards hazards) {
        emittingTimer = EMIT_TIMEOUT;
        FluidStack carried = gasStack(recipeGas, 1);
        carriedGas = carried.isEmpty() ? input.copyWithAmount(1) : carried;
        beamParticle = particles.particleFor(input);
        int ceiling = ceilingGap();
        double topY = worldPosition.getY() + ceiling;
        if (divertTimer > 0 && beamBounds != null) {
            divertGas(input, particles, hazards, topY);
            return;
        }
        if (!gas.isEmpty() && level.getBlockEntity(worldPosition.above(ceiling)) instanceof ExhaustHoodBlockEntity hood)
            hood.addGas(gas);
        if (ceiling > 1)
            emitParticles(topY, particles.particleFor(input), particles);
        applyHazards(topY, input, hazards);
    }

    private void divertGas(FluidStack input, GasParticles particles, GasHazards hazards, double topY) {
        double capY = Math.min(topY, beamCrossingY);
        if (capY > worldPosition.getY() + 1)
            emitParticles(capY, particles.particleFor(input), particles);
        if (!hazards.isEmpty() && effectCooldown <= 0) {
            effectCooldown = EFFECT_INTERVAL;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, beamBounds)) {
                hazards.apply(entity, input);
                triggerGasExposure(entity);
            }
        }
    }

    private static void triggerGasExposure(LivingEntity entity) {
        if (entity instanceof ServerPlayer player)
            CNCriteriaTriggers.GAS_EXPOSURE.get().trigger(player);
    }

    public void divert(AABB bounds, int crossingY) {
        beamBounds = bounds;
        beamCrossingY = crossingY;
        divertTimer = DIVERT_TIMEOUT;
    }

    public boolean isEmittingGas() {
        return level != null && level.isClientSide ? emitting : emittingTimer > 0;
    }

    public int getBeamColor() {
        return beamParticle == null ? -1 : beamColorOf(beamParticle);
    }

    public ParticleOptions getBeamParticle() {
        return beamParticle;
    }

    private static int beamColorOf(ParticleOptions particle) {
        if (particle instanceof ColorParticleOption color)
            return (Math.round(color.getRed() * 255) << 16)
                    | (Math.round(color.getGreen() * 255) << 8)
                    | Math.round(color.getBlue() * 255);
        if (particle.getType() == ParticleTypes.SMOKE || particle.getType() == ParticleTypes.LARGE_SMOKE)
            return 0x686868;
        return 0xDADED8;
    }

    public FluidStack getCarriedGas() {
        return carriedGas.copy();
    }

    public int gasColumnHeight() {
        return ceilingGap();
    }

    private int ceilingGap() {
        for (int gap = 1; gap < MAX_HOOD_GAP; gap++)
            if (!isGasPermeable(level, worldPosition.above(gap))) return gap;
        return MAX_HOOD_GAP;
    }

    public static boolean isGasPermeable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ExhaustHoodBlockEntity || blockEntity instanceof EvaporationBasinBlockEntity) return false;
        return !state.isSolidRender(level, pos);
    }

    @Nullable
    public static EvaporationBasinBlockEntity emittingBasinBelow(Level level, BlockPos cell) {
        for (int depth = 1; depth <= MAX_HOOD_GAP; depth++) {
            BlockPos below = cell.below(depth);
            if (level.getBlockEntity(below) instanceof EvaporationBasinBlockEntity basin)
                return basin.isEmittingGas() && depth <= basin.gasColumnHeight() ? basin : null;
            if (!isGasPermeable(level, below)) return null;
        }
        return null;
    }

    private void emitParticles(double topY, ParticleOptions particle, GasParticles particles) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        RandomSource rand = serverLevel.random;
        double baseY = worldPosition.getY() + 0.9;
        double height = Math.max(0, topY - baseY - 0.75);
        double cx = worldPosition.getX() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        for (int n = 0; n < particles.count(); n++) {
            double y = baseY + rand.nextDouble() * height;
            double x = cx + (rand.nextDouble() - 0.5) * particles.spread();
            double z = cz + (rand.nextDouble() - 0.5) * particles.spread();
            serverLevel.sendParticles(particle, x, y, z, 0, 0.0, 1.0, 0.0, particles.speed());
        }
    }

    private void applyHazards(double topY, FluidStack input, GasHazards hazards) {
        if (hazards.isEmpty() || effectCooldown > 0) return;
        effectCooldown = EFFECT_INTERVAL;
        AABB column = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, topY, worldPosition.getZ() + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, column)) {
            hazards.apply(entity, input);
            triggerGasExposure(entity);
        }
    }

    private void setBlocked(boolean value) {
        if (blocked == value) return;
        blocked = value;
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FluidTank in = input();
        FluidTank out = output();
        GoggleTankPanel.header(tooltip);
        GoggleTankPanel.append(tooltip, "gui.goggles.tank_input", in.getFluid(), in.getCapacity());
        GoggleTankPanel.append(tooltip, "gui.goggles.tank_output", out.getFluid(), out.getCapacity());
        appendStats(tooltip, in.getFluid(), "gui.goggles.basin_input");
        appendStats(tooltip, out.getFluid(), "gui.goggles.basin_output");
        if (blocked)
            new LangBuilder(CreateNuclearized.MODID).translate("gui.goggles.basin_blocked")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        return true;
    }

    private static void appendStats(List<Component> tooltip, FluidStack stack, String headerKey) {
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            SaltStatsTooltip.append(tooltip, stats, ReactorFuel.forOutput(stack.getFluid()), headerKey);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Blocked", blocked);
        tag.putBoolean("Emitting", emitting);
        if (beamParticle != null)
            ParticleTypes.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), beamParticle)
                    .result().ifPresent(t -> tag.put("BeamParticle", t));
        if (clientPacket) return;
        tag.putDouble("DegasBudget", degasBudget);
        tag.put("DegasBatch", degasBatch.saveOptional(registries));
        tag.putDouble("GasCarry", gasCarry);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        blocked = tag.getBoolean("Blocked");
        emitting = tag.getBoolean("Emitting");
        beamParticle = tag.contains("BeamParticle")
                ? ParticleTypes.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag.get("BeamParticle")).result().orElse(null)
                : null;
        if (clientPacket) return;
        degasBudget = tag.getDouble("DegasBudget");
        degasBatch = FluidStack.parseOptional(registries, tag.getCompound("DegasBatch"));
        gasCarry = tag.getDouble("GasCarry");
    }

    @Override
    protected void transferContentsTo(EvaporationBasinBlockEntity target) {
        super.transferContentsTo(target);
        ItemStack own = filtering.ownFilter();
        if (own.isEmpty()) return;
        if (target.filtering.ownFilter().isEmpty())
            target.filtering.setOwnFilter(own);
        else if (own.getItem() instanceof FilterItem)
            Block.popResource(level, worldPosition, own.copy());
        filtering.setOwnFilter(ItemStack.EMPTY);
    }

    private void releaseStaleFilter() {
        if (isController() || filtering.ownFilter().isEmpty()) return;
        SharedFilteringBehaviour shared = getControllerBE().filtering;
        if (shared != filtering && shared.ownFilter().isEmpty())
            shared.setOwnFilter(filtering.ownFilter());
        filtering.setOwnFilter(ItemStack.EMPTY);
    }

    public FilteringBehaviour getFiltering() {
        return filtering;
    }

    public BasinValueBox getValueBox() {
        return valueBox;
    }

    private static class SharedFilteringBehaviour extends FilteringBehaviour {

        private final EvaporationBasinBlockEntity basin;

        SharedFilteringBehaviour(EvaporationBasinBlockEntity basin, ValueBoxTransform slot) {
            super(basin, slot);
            this.basin = basin;
        }

        private SharedFilteringBehaviour owner() {
            return basin.getControllerBE().filtering;
        }

        ItemStack ownFilter() {
            return filter.item();
        }

        void setOwnFilter(ItemStack stack) {
            super.setFilter(stack);
        }

        @Override
        public boolean setFilter(ItemStack stack) {
            SharedFilteringBehaviour owner = owner();
            return owner == this ? super.setFilter(stack) : owner.setFilter(stack);
        }

        @Override
        public ItemStack getFilter() {
            SharedFilteringBehaviour owner = owner();
            return owner == this ? ownFilter() : owner.ownFilter();
        }

        @Override
        public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
            nbt.put("Filter", ownFilter().saveOptional(registries));
            nbt.putInt("FilterAmount", count);
            nbt.putBoolean("UpTo", upTo);
        }

        @Override
        public void destroy() {
            if (owner() == this)
                super.destroy();
        }
    }

    public static class BasinValueBox extends ValueBoxTransform.Sided {

        private static final double HEIGHT = 12 / 16.0;
        private static final double FACE_GAP = 0.05 / 16;

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 12, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return state.getValue(CellSlot.PROPERTY).isOuter(direction);
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction side = getSide();
            if (!isSideActive(state, side) || !(level.getBlockEntity(pos) instanceof EvaporationBasinBlockEntity basin)) return null;
            Vec3 offset = centeredOffset(basin, pos, side);
            double along = alongFace(offset, side);
            return along < 0 || along > 1 ? null : offset;
        }

        public boolean drawsItem(LevelAccessor level, BlockPos pos, BlockState state) {
            Vec3 offset = getLocalOffset(level, pos, state);
            return offset != null && alongFace(offset, getSide()) < 1;
        }

        private static Vec3 centeredOffset(EvaporationBasinBlockEntity basin, BlockPos pos, Direction side) {
            EvaporationBasinBlockEntity ctrl = basin.getControllerBE();
            BlockPos origin = ctrl.getBlockPos();
            double half = ctrl.getWidth() / 2.0;
            double reach = half + FACE_GAP;
            return new Vec3(origin.getX() - pos.getX() + half + side.getStepX() * reach, HEIGHT,
                    origin.getZ() - pos.getZ() + half + side.getStepZ() * reach);
        }

        private static double alongFace(Vec3 offset, Direction side) {
            return side.getAxis() == Direction.Axis.X ? offset.z : offset.x;
        }
    }
}
