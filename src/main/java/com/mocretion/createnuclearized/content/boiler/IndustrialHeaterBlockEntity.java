package com.mocretion.createnuclearized.content.boiler;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.menu.CNIcons;
import com.mocretion.createnuclearized.registry.CNRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public class IndustrialHeaterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 1000;
    public static final int BUFFER_CAPACITY = 500_000;
    private static final int GEN_INTERVAL = 5;
    private static final int GEN_BATCH = 100;
    private static final int WORK_LINGER = 10;

    public enum DrainSource implements INamedIconOptions {
        OUTPUT(CNIcons.SNOW_FLAKE),
        INPUT(CNIcons.FIRE);

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
        Fluid liquid = Fluids.EMPTY;

        boolean isEmpty() {
            return heat <= 0 || liquid == Fluids.EMPTY;
        }

        void clear() {
            heat = 0;
            liquid = Fluids.EMPTY;
        }
    }

    private FluidTank inputTank;
    private FluidTank outputTank;
    private IFluidHandler fluidHandler;

    @Nullable
    private ScrollOptionBehaviour<DrainSource> drainSource;

    private final HeatStore[] buffers = { new HeatStore(), new HeatStore() };
    private int genCooldown;
    private int workTimer;

    private boolean active;

    private final LerpedFloat headAnimation = LerpedFloat.linear();
    private final LerpedFloat headAngle = LerpedFloat.angular();

    public IndustrialHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        headAngle.startWithValue(0);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        SmartFluidTankBehaviour input = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, CAPACITY, false)
                .forbidExtraction();
        SmartFluidTankBehaviour output = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, CAPACITY, false)
                .forbidInsertion();
        inputTank = input.getPrimaryHandler().setValidator(this::isHeater);
        outputTank = output.getPrimaryHandler();
        fluidHandler = new BurnerFluidHandler();
        drainSource = new ScrollOptionBehaviour<>(DrainSource.class,
                Component.translatable("createnuclearized.industrial_heater.drain_source"), this,
                new CenteredSideValueBoxTransform((state, dir) -> dir.getAxis().isHorizontal()));
        behaviours.add(input);
        behaviours.add(output);
        behaviours.add(drainSource);
    }

    private FluidTank drainTank() {
        return drainSource != null && drainSource.get() == DrainSource.INPUT ? inputTank : outputTank;
    }

    private class BurnerFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return (index == 0 ? inputTank : outputTank).getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return (index == 0 ? inputTank : outputTank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isHeater(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return isHeater(resource) ? inputTank.fill(resource, action) : 0;
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

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            animateHead();
            spawnParticles();
        } else {
            tickWork();
        }
    }

    private void spawnParticles() {
        if (level == null) return;
        RandomSource r = level.getRandom();
        if (r.nextInt(4) != 0) return;

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f).multiply(1, 0, 1));
        boolean empty = level.getBlockState(worldPosition.above())
                .getCollisionShape(level, worldPosition.above()).isEmpty();

        if (empty || r.nextInt(8) == 0)
            level.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0, 0, 0);

        if (!active) return;

        double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f).multiply(1, .25f, 1).normalize()
                .scale((empty ? .25f : .5) + r.nextDouble() * .125f)).add(0, .5, 0);
        level.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
    }

    private void animateHead() {
        float look = IndustrialHeaterClient.lookAngle(worldPosition);
        float target = Float.isNaN(look) ? 0 : look;
        target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
        headAngle.chase(target, 0.25f, Chaser.exp(5));
        headAngle.tickChaser();

        headAnimation.chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        headAnimation.tickChaser();
    }

    private void tickWork() {
        if (genCooldown > 0)
            genCooldown--;
        else {
            generate();
            genCooldown = GEN_INTERVAL;
        }
        if (workTimer > 0)
            workTimer--;
        setActive(workTimer > 0);
    }

    private void generate() {
        FluidStack heater = inputTank.getFluid();
        if (heater.isEmpty()) return;
        HeatGenerationRecipe recipe = findGeneration(heater);
        if (recipe == null) return;
        int perMb = recipe.heatFor(heater);
        if (perMb <= 0) return;

        Fluid liquid = heater.getFluid();
        int byHeat = (int) (heatRoomFor(liquid) / perMb);
        int amount = Math.min(Math.min(GEN_BATCH, heater.getAmount()), byHeat);
        if (amount <= 0) return;

        FluidStack result = recipe.output(heater, amount);
        if (!result.isEmpty()) {
            int fit = outputTank.fill(result, FluidAction.SIMULATE);
            if (fit <= 0) return;
            if (fit < amount) amount = fit;
            result = recipe.output(heater, amount);
        }

        addHeat(liquid, (double) amount * perMb);
        inputTank.drain(amount, FluidAction.EXECUTE);
        if (!result.isEmpty())
            outputTank.fill(result, FluidAction.EXECUTE);
        workTimer = WORK_LINGER;
        setActive(true);
        notifyUpdate();
    }

    private double heatRoomFor(Fluid liquid) {
        double room = 0;
        for (HeatStore b : buffers)
            if (b.isEmpty() || b.liquid == liquid) room += BUFFER_CAPACITY - b.heat;
        return room;
    }

    private void addHeat(Fluid liquid, double amount) {
        HeatStore primary = bufferFor(liquid);
        if (primary == null) return;
        amount = fillBuffer(primary, liquid, amount);
        if (amount <= 0) return;
        for (HeatStore b : buffers) {
            if (b == primary) continue;
            if (b.isEmpty() || b.liquid == liquid)
                amount = fillBuffer(b, liquid, amount);
        }
    }

    @Nullable
    private HeatStore bufferFor(Fluid liquid) {
        for (HeatStore b : buffers)
            if (!b.isEmpty() && b.liquid == liquid) return b;
        for (HeatStore b : buffers)
            if (b.isEmpty()) return b;
        return null;
    }

    private static double fillBuffer(HeatStore b, Fluid liquid, double amount) {
        if (b.isEmpty()) b.liquid = liquid;
        double add = Math.min(amount, BUFFER_CAPACITY - b.heat);
        b.heat += add;
        return amount - add;
    }

    public boolean hasHeatFor(BoilingRecipe recipe) {
        return eligibleHeat(recipe.heater()) > 0;
    }

    public int boil(int maxWater, BoilingRecipe recipe) {
        double perMb = recipe.heatPerMb();
        if (perMb <= 0) return 0;
        double available = eligibleHeat(recipe.heater());
        int amount = (int) Math.min(maxWater, Math.floor(available / perMb));
        if (amount <= 0) return 0;

        drainHeat(recipe.heater(), amount * perMb);
        workTimer = WORK_LINGER;
        setActive(true);
        notifyUpdate();
        return amount;
    }

    private double eligibleHeat(Optional<FluidIngredient> required) {
        double sum = 0;
        for (HeatStore b : buffers)
            if (!b.isEmpty() && matchesRequirement(required, b.liquid)) sum += b.heat;
        return sum;
    }

    private void drainHeat(Optional<FluidIngredient> required, double needed) {
        for (HeatStore b : buffers) {
            if (needed <= 0) break;
            if (b.isEmpty() || !matchesRequirement(required, b.liquid)) continue;
            double take = Math.min(b.heat, needed);
            b.heat -= take;
            needed -= take;
            if (b.heat <= 0) b.clear();
        }
    }

    private static boolean matchesRequirement(Optional<FluidIngredient> required, Fluid liquid) {
        return required.map(r -> r.test(new FluidStack(liquid.builtInRegistryHolder(), 1))).orElse(true);
    }

    private void setActive(boolean value) {
        if (active == value) return;
        active = value;
        level.setBlockAndUpdate(worldPosition,
                getBlockState().setValue(IndustrialHeaterBlock.HEAT_LEVEL, IndustrialHeaterBlock.heatLevel(value)));
        notifyUpdate();
    }

    @Nullable
    private HeatGenerationRecipe findGeneration(FluidStack stack) {
        if (level == null || stack.isEmpty()) return null;
        for (RecipeHolder<HeatGenerationRecipe> holder : level.getRecipeManager().getAllRecipesFor(CNRecipeTypes.HEAT_GENERATION.get()))
            if (holder.value().accepts(stack)) return holder.value();
        return null;
    }

    private boolean isHeater(FluidStack stack) {
        return findGeneration(stack) != null;
    }

    public FluidStack getBufferDisplay(int index) {
        HeatStore b = buffers[index];
        if (b.isEmpty()) return FluidStack.EMPTY;
        return new FluidStack(b.liquid.builtInRegistryHolder(), CAPACITY);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, fluidHandler);
        new LangBuilder(CreateNuclearized.MODID).translate("gui.goggles.heater_buffers")
                .style(ChatFormatting.WHITE).forGoggles(tooltip);
        for (HeatStore b : buffers)
            bufferLine(tooltip, b);
        return true;
    }

    private static void bufferLine(List<Component> tooltip, HeatStore b) {
        if (b.isEmpty()) {
            new LangBuilder(CreateNuclearized.MODID).translate("gui.goggles.heater_buffer_empty")
                    .style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return;
        }
        Component name = new FluidStack(b.liquid.builtInRegistryHolder(), 1).getHoverName();
        new LangBuilder(CreateNuclearized.MODID).add(name.copy().withStyle(ChatFormatting.GRAY))
                .text(ChatFormatting.GRAY, ": ")
                .add(CreateLang.number((int) b.heat).style(ChatFormatting.GOLD).text(ChatFormatting.GOLD, " heat"))
                .forGoggles(tooltip, 1);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Active", active);
        ListTag list = new ListTag();
        for (HeatStore b : buffers) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("Heat", b.heat);
            entry.putString("Liquid", BuiltInRegistries.FLUID.getKey(b.liquid).toString());
            list.add(entry);
        }
        tag.put("Buffers", list);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        active = tag.getBoolean("Active");
        ListTag list = tag.getList("Buffers", Tag.TAG_COMPOUND);
        for (int i = 0; i < buffers.length; i++) {
            buffers[i].clear();
            if (i >= list.size()) continue;
            CompoundTag entry = list.getCompound(i);
            double heat = entry.getDouble("Heat");
            Fluid liquid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(entry.getString("Liquid")));
            if (heat > 0 && liquid != Fluids.EMPTY) {
                buffers[i].heat = heat;
                buffers[i].liquid = liquid;
            }
        }
    }
}
