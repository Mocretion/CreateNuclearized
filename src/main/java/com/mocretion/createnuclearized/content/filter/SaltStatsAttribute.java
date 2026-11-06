package com.mocretion.createnuclearized.content.filter;

import java.util.List;
import java.util.function.BiPredicate;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mocretion.createnuclearized.registry.CNFluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class SaltStatsAttribute implements ItemAttribute {

    public enum Kind {
        HEAT("has_heat", (fuel, stats) -> stats.heat() > 0),
        UNCONSUMED("has_unconsumed",
                (fuel, stats) -> fuel != null && fuel.unconsumed() != null && stats.amount(fuel.unconsumed()) > 0),
        BRED("has_bred", (fuel, stats) -> fuel != null && fuel.bred() != null && stats.amount(fuel.bred()) > 0),
        OFF_GAS("has_off_gas", (fuel, stats) -> stats.offGas() > 0);

        private final String id;
        private final BiPredicate<ReactorFuel, SaltStats> test;

        Kind(String id, BiPredicate<ReactorFuel, SaltStats> test) {
            this.id = id;
            this.test = test;
        }
    }

    private final Type type;
    private final Kind kind;

    private SaltStatsAttribute(Type type, Kind kind) {
        this.type = type;
        this.kind = kind;
    }

    @Override
    public boolean appliesTo(ItemStack stack, Level world) {
        return appliesTo(contents(stack));
    }

    public boolean appliesTo(FluidStack contents) {
        if (contents.isEmpty()) return false;
        SaltStats stats = contents.get(CNDataComponents.SALT_STATS.get());
        if (stats == null) return false;
        return kind.test.test(ReactorFuel.forOutput(contents.getFluid()), stats);
    }

    @Override
    public ItemAttributeType getType() {
        return type;
    }

    @Override
    public String getTranslationKey() {
        return kind.id;
    }

    @Override
    public MutableComponent format(boolean inverted) {
        return Component.translatable("createnuclearized.item_attributes." + kind.id + (inverted ? ".inverted" : ""));
    }

    private static FluidStack contents(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null || handler.getTanks() == 0) return FluidStack.EMPTY;
        return handler.getFluidInTank(0);
    }

    public static void register(RegisterEvent event) {
        event.register(CreateRegistries.ITEM_ATTRIBUTE_TYPE, helper -> {
            for (Kind kind : Kind.values())
                helper.register(ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, kind.id), new Type(kind));
        });
    }

    public static final class Type implements ItemAttributeType {
        private final SaltStatsAttribute attribute;

        private Type(Kind kind) {
            this.attribute = new SaltStatsAttribute(this, kind);
        }

        @Override
        public ItemAttribute createAttribute() {
            return attribute;
        }

        @Override
        public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
            FluidStack contents = contents(stack);
            if (contents.isEmpty()) return List.of();
            var holder = contents.getFluid().builtInRegistryHolder();
            if (holder.is(CNFluids.SALTS) || holder.is(CNFluids.RADIOACTIVE_METAL))
                return List.of(attribute);
            return List.of();
        }

        @Override
        public MapCodec<? extends ItemAttribute> codec() {
            return Codec.unit(attribute).fieldOf("value");
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
            return StreamCodec.unit(attribute);
        }
    }
}
