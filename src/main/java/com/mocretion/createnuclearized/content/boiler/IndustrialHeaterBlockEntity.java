package com.mocretion.createnuclearized.content.boiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.menu.CNIcons;
import com.mocretion.createnuclearized.content.radiation.CNDamageTypes;
import com.mocretion.createnuclearized.content.recipe.CachedRecipes;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class IndustrialHeaterBlockEntity extends CellMultiblockBlockEntity<IndustrialHeaterBlockEntity>
        implements IHaveGoggleInformation {

    public static final int CAPACITY = 200;
    public static final int BUFFER_CAPACITY = 1_000_000;
    private static final int GEN_INTERVAL = 5;
    private static final int GEN_BATCH = 100;
    private static final int WORK_LINGER = 10;
    private static final int DAMAGE_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;
    private static final int HEAT_PER_WATER_MB = 200;
    private static final int HEAT_PER_COMPARATOR_LEVEL = 50_000;

    public enum DrainSource implements INamedIconOptions {
        INPUT(CNIcons.FIRE),
        OUTPUT(CNIcons.SNOW_FLAKE);

        private final AllIcons icon;

        DrainSource(AllIcons icon) {
            this.icon = icon;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return "createnuclearized.industrial_heater.drain_source." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final class HeatStore {
        double heat;
        net.minecraft.world.level.material.Fluid liquid = Fluids.EMPTY;

        boolean isEmpty() {
            return heat <= 0 || liquid == Fluids.EMPTY;
        }

        void set(net.minecraft.world.level.material.Fluid liquid, double heat) {
            this.liquid = liquid;
            this.heat = heat;
        }

        void remove(double amount) {
            heat -= Math.min(heat, amount);
            if (heat <= 0) clear();
        }

        void clear() {
            set(Fluids.EMPTY, 0);
        }
    }

    private IFluidHandler fluidCapability;
    private ScrollOptionBehaviour<DrainSource> drainSource;

    private final HeatStore buffer = new HeatStore();
    private int genCooldown;
    private int workTimer;
    private int damageCooldown;
    private int syncCooldown;
    private boolean syncPending;
    private int lastComparatorSignal = -1;

    public IndustrialHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        SmartFluidTankBehaviour input = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, CAPACITY, false)
                .forbidExtraction();
        SmartFluidTankBehaviour output = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, CAPACITY, false)
                .forbidInsertion();
        inputTank = input.getPrimaryHandler().setValidator(this::isHeater);
        outputTank = output.getPrimaryHandler();
        drainSource = new ScrollOptionBehaviour<>(DrainSource.class,
                Component.translatable("createnuclearized.industrial_heater.drain_source"), this,
                new CenteredSideValueBoxTransform((state, dir) -> dir.getAxis().isHorizontal()));
        drainSource.value = DrainSource.OUTPUT.ordinal();
        behaviours.add(input);
        behaviours.add(output);
        behaviours.add(drainSource);
        refreshCapability();
    }

    private final class BurnerFluidHandler implements IFluidHandler {
        private final FluidTank in;
        private final FluidTank out;

        BurnerFluidHandler(FluidTank in, FluidTank out) {
            this.in = in;
            this.out = out;
        }

        private FluidTank drainTank() {
            return drainSource.get() == DrainSource.INPUT ? in : out;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return (index == 0 ? in : out).getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return (index == 0 ? in : out).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && (isCoolant(stack) || isHeater(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int cooled = coolWithWater(resource, action);
            return cooled > 0 ? cooled : in.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidTank tank = drainTank();
            if (!FluidStack.isSameFluidSameComponents(resource, tank.getFluid())) return FluidStack.EMPTY;
            return tank.drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drainTank().drain(maxDrain, action);
        }
    }

    public IFluidHandler getFluidHandler() {
        return fluidCapability;
    }

    @Override
    protected Class<IndustrialHeaterBlockEntity> cellClass() {
        return IndustrialHeaterBlockEntity.class;
    }

    @Override
    protected int capacityPerBlock() {
        return CAPACITY;
    }

    @Override
    protected void clientTick() {
        spawnParticles();
    }

    @Override
    protected void serverTick() {
        if (isController())
            tickWork();
    }

    private void spawnParticles() {
        IndustrialHeaterBlock.Temperature t = getBlockState().getValue(IndustrialHeaterBlock.TEMPERATURE);
        if (t == IndustrialHeaterBlock.Temperature.LOW) return;

        RandomSource r = level.getRandom();
        if (r.nextInt(4) != 0) return;

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        boolean empty = level.getBlockState(worldPosition.above())
                .getCollisionShape(level, worldPosition.above()).isEmpty();
        double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f).multiply(1, .25f, 1).normalize()
                .scale((empty ? .25f : .5) + r.nextDouble() * .125f)).add(0, .5, 0);
        level.addParticle(t == IndustrialHeaterBlock.Temperature.HIGH
                ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                v2.x, v2.y, v2.z, 0, yMotion, 0);
    }

    private void tickWork() {
        if (--genCooldown <= 0) {
            genCooldown = GEN_INTERVAL;
            generate();
        }
        if (workTimer > 0)
            workTimer--;
        setActive(workTimer > 0);
        syncTemperature();
        tickDamage();
        updateComparators();
        flushSync();
    }

    private void markWorking() {
        workTimer = WORK_LINGER;
        queueSync();
    }

    private void queueSync() {
        setChanged();
        syncPending = true;
    }

    private void flushSync() {
        if (syncCooldown > 0)
            syncCooldown--;
        if (!syncPending || syncCooldown > 0) return;
        syncPending = false;
        syncCooldown = SYNC_INTERVAL;
        sendData();
    }

    private void tickDamage() {
        if (damageCooldown > 0 && --damageCooldown > 0) return;
        if (buffer.isEmpty()) return;

        AABB top = new AABB(worldPosition.above()).expandTowards(width - 1, 0, width - 1);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, top);
        if (entities.isEmpty()) return;
        List<AABB> exposed = exposedCells();

        IndustrialHeaterBlock.Temperature temperature = getBlockState().getValue(IndustrialHeaterBlock.TEMPERATURE);
        DamageSource source = CNDamageTypes.scorchingHeat(level);
        boolean hit = false;
        for (LivingEntity e : entities)
            if (exposed.stream().anyMatch(e.getBoundingBox()::intersects) && e.hurt(source, temperature.contactDamage))
                hit = true;
        if (!hit) return;
        buffer.remove(temperature.contactHeatLoss);
        damageCooldown = DAMAGE_INTERVAL;
        queueSync();
    }

    private List<AABB> exposedCells() {
        List<AABB> cells = new ArrayList<>();
        forEachCell(pos -> {
            BlockPos above = pos.above();
            if (!level.getBlockState(above).canOcclude() && !(level.getBlockEntity(above) instanceof EvaporationBasinBlockEntity))
                cells.add(new AABB(above));
        });
        return cells;
    }

    private void syncTemperature() {
        IndustrialHeaterBlock.Temperature target = IndustrialHeaterBlock.temperatureFor(buffer.heat / bufferCapacity());
        if (getBlockState().getValue(IndustrialHeaterBlock.TEMPERATURE) != target)
            applyToAllCells(state -> state.setValue(IndustrialHeaterBlock.TEMPERATURE, target));
    }

    private void setActive(boolean active) {
        HeatLevel target = IndustrialHeaterBlock.heatLevel(active);
        if (getBlockState().getValue(IndustrialHeaterBlock.HEAT_LEVEL) != target)
            applyToAllCells(state -> state.setValue(IndustrialHeaterBlock.HEAT_LEVEL, target));
    }

    private void updateComparators() {
        int signal = comparatorSignal();
        if (signal == lastComparatorSignal) return;
        lastComparatorSignal = signal;
        Block block = getBlockState().getBlock();
        forEachCell(pos -> level.updateNeighbourForOutputSignal(pos, block));
    }

    public int comparatorSignal() {
        IndustrialHeaterBlockEntity ctrl = getControllerBE();
        return Math.min(15, (int) (ctrl.buffer.heat / ((double) HEAT_PER_COMPARATOR_LEVEL * ctrl.clusterBlocks())));
    }

    private int bufferCapacity() {
        return BUFFER_CAPACITY * clusterBlocks();
    }

    private void applyToAllCells(UnaryOperator<BlockState> op) {
        forEachCell(pos -> {
            BlockState cur = level.getBlockState(pos);
            if (!(cur.getBlock() instanceof IndustrialHeaterBlock)) return;
            BlockState next = op.apply(cur);
            if (cur != next)
                level.setBlock(pos, next, CELL_UPDATE_FLAGS);
        });
    }

    private void generate() {
        FluidStack heater = inputTank.getFluid();
        HeatGenerationRecipe recipe = findGeneration(heater);
        if (recipe == null) return;
        int perMb = recipe.heatFor(heater);

        net.minecraft.world.level.material.Fluid liquid = heater.getFluid();
        int byHeat = (int) (heatRoomFor(liquid) / perMb);
        int amount = Math.min(Math.min(GEN_BATCH * clusterBlocks(), heater.getAmount()), byHeat);
        if (amount <= 0) return;

        FluidStack result = recipe.output(heater, amount);
        if (!result.isEmpty()) {
            amount = Math.min(amount, outputTank.fill(result, FluidAction.SIMULATE));
            if (amount <= 0) return;
            result = result.copyWithAmount(amount);
        }

        if (buffer.isEmpty())
            buffer.liquid = liquid;
        buffer.heat += (double) amount * perMb;
        inputTank.drain(amount, FluidAction.EXECUTE);
        if (!result.isEmpty())
            outputTank.fill(result, FluidAction.EXECUTE);
        markWorking();
    }

    private double heatRoomFor(net.minecraft.world.level.material.Fluid liquid) {
        if (!buffer.isEmpty() && buffer.liquid != liquid) return 0;
        return bufferCapacity() - buffer.heat;
    }

    public boolean hasHeatFor(BoilingRecipe recipe) {
        IndustrialHeaterBlockEntity ctrl = getControllerBE();
        if (ctrl != this) return ctrl.hasHeatFor(recipe);
        return eligibleHeat(recipe.heater()) > 0;
    }

    public int boil(int maxWater, BoilingRecipe recipe) {
        IndustrialHeaterBlockEntity ctrl = getControllerBE();
        if (ctrl != this) return ctrl.boil(maxWater, recipe);
        double perMb = recipe.heatPerMb();
        int amount = (int) Math.min(maxWater, eligibleHeat(recipe.heater()) / perMb);
        if (amount <= 0) return 0;

        buffer.remove(amount * perMb);
        markWorking();
        return amount;
    }

    private double eligibleHeat(Optional<FluidIngredient> required) {
        if (buffer.isEmpty()) return 0;
        boolean matches = required.map(r -> r.test(new FluidStack(buffer.liquid.builtInRegistryHolder(), 1))).orElse(true);
        return matches ? buffer.heat : 0;
    }

    private HeatGenerationRecipe findGeneration(FluidStack stack) {
        if (level == null || stack.isEmpty()) return null;
        for (RecipeHolder<HeatGenerationRecipe> holder : CachedRecipes.of(level, CNRecipeTypes.HEAT_GENERATION.get()))
            if (holder.value().accepts(stack)) return holder.value();
        return null;
    }

    private boolean isHeater(FluidStack stack) {
        return findGeneration(stack) != null;
    }

    private static boolean isCoolant(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
    }

    private int coolWithWater(FluidStack resource, FluidAction action) {
        if (!isCoolant(resource)) return 0;
        IndustrialHeaterBlockEntity ctrl = getControllerBE();
        if (ctrl != this) return ctrl.coolWithWater(resource, action);
        if (buffer.isEmpty()) return 0;
        if (action.execute()) {
            buffer.remove((double) resource.getAmount() * HEAT_PER_WATER_MB);
            markWorking();
        }
        return resource.getAmount();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        IndustrialHeaterBlockEntity ctrl = getControllerBE();
        if (ctrl != this) return ctrl.addToGoggleTooltip(tooltip, isPlayerSneaking);
        GoggleTankPanel.header(tooltip);
        GoggleTankPanel.append(tooltip, "gui.goggles.tank_input", inputTank.getFluid(), inputTank.getCapacity());
        GoggleTankPanel.append(tooltip, "gui.goggles.tank_output", outputTank.getFluid(), outputTank.getCapacity());
        new LangBuilder(CreateNuclearized.MODID).translate("gui.goggles.heater_buffers")
                .style(ChatFormatting.WHITE).forGoggles(tooltip);
        if (buffer.isEmpty()) {
            CreateLang.translate("gui.goggles.fluid_container.capacity")
                    .add(heatAmount(bufferCapacity()))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
            return true;
        }
        Component name = new FluidStack(buffer.liquid.builtInRegistryHolder(), 1).getHoverName();
        new LangBuilder(CreateNuclearized.MODID).add(name.copy().withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(heatAmount((int) buffer.heat))
                .forGoggles(tooltip, 1);
        return true;
    }

    private static LangBuilder heatAmount(int heat) {
        return CreateLang.number(heat).space()
                .add(new LangBuilder(CreateNuclearized.MODID).translate("generic.unit.heat"))
                .style(ChatFormatting.GOLD);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Heat", buffer.heat);
        tag.putString("Liquid", BuiltInRegistries.FLUID.getKey(buffer.liquid).toString());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        buffer.clear();
        if (tag.contains("Buffers", Tag.TAG_LIST))
            readLegacyBuffers(tag.getList("Buffers", Tag.TAG_COMPOUND));
        else
            readHeat(tag);
    }

    private void readHeat(CompoundTag tag) {
        double heat = tag.getDouble("Heat");
        net.minecraft.world.level.material.Fluid liquid = fluidOf(tag.getString("Liquid"));
        if (heat > 0 && liquid != Fluids.EMPTY)
            buffer.set(liquid, heat);
    }

    private void readLegacyBuffers(ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            double heat = entry.getDouble("Heat");
            net.minecraft.world.level.material.Fluid liquid = fluidOf(entry.getString("Liquid"));
            if (heat <= 0 || liquid == Fluids.EMPTY) continue;
            if (buffer.isEmpty())
                buffer.set(liquid, heat);
            else if (buffer.liquid == liquid)
                buffer.heat += heat;
        }
    }

    private static net.minecraft.world.level.material.Fluid fluidOf(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getOptional(location).orElse(Fluids.EMPTY);
    }

    @Override
    protected void refreshCapability() {
        IndustrialHeaterBlockEntity ctrl = getControllerBE();
        fluidCapability = new BurnerFluidHandler(ctrl.inputTank, ctrl.outputTank);
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    @Override
    protected void transferContentsTo(IndustrialHeaterBlockEntity target) {
        super.transferContentsTo(target);
        if (buffer.isEmpty()) return;
        if (target.buffer.isEmpty())
            target.buffer.set(buffer.liquid, buffer.heat);
        else if (target.buffer.liquid == buffer.liquid)
            target.buffer.heat += buffer.heat;
        else return;
        buffer.clear();
        target.setChanged();
        target.sendData();
    }

    @Override
    protected BlockState copyFromController(BlockState state, BlockState controllerState) {
        return state.setValue(IndustrialHeaterBlock.TEMPERATURE, controllerState.getValue(IndustrialHeaterBlock.TEMPERATURE))
                .setValue(IndustrialHeaterBlock.HEAT_LEVEL, controllerState.getValue(IndustrialHeaterBlock.HEAT_LEVEL));
    }

    @Override
    protected void distributeContents(int keptBlocks) {
        super.distributeContents(keptBlocks);
        double heat = buffer.isEmpty() ? 0 : Math.max(0, buffer.heat - keptBlocks * BUFFER_CAPACITY);
        for (IndustrialHeaterBlockEntity sibling : siblings()) {
            if (heat <= 0) return;
            double give = Math.min(sibling.cellHeatRoomFor(buffer.liquid), heat);
            if (give <= 0) continue;
            sibling.buffer.set(buffer.liquid, sibling.buffer.heat + give);
            heat -= give;
            sibling.setChanged();
            sibling.sendData();
        }
    }

    private double cellHeatRoomFor(net.minecraft.world.level.material.Fluid liquid) {
        if (buffer.isEmpty()) return BUFFER_CAPACITY;
        return buffer.liquid == liquid ? BUFFER_CAPACITY - buffer.heat : 0;
    }

    @Override
    protected void applyTankSize(int blocks) {
        super.applyTankSize(blocks);
        buffer.heat = Math.min(buffer.heat, (double) BUFFER_CAPACITY * blocks);
    }
}
