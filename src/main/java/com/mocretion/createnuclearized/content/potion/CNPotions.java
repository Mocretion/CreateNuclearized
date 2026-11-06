package com.mocretion.createnuclearized.content.potion;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.radiation.CNEffects;
import com.mocretion.createnuclearized.registry.CNItems;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = CreateNuclearized.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CNPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, CreateNuclearized.MODID);

    private static final int SECOND = 20;

    public static final Holder<Potion> RADIATION_SICKNESS = POTIONS.register("radiation_sickness",
            () -> radiationSickness(45 * SECOND, 0));
    public static final Holder<Potion> LONG_RADIATION_SICKNESS = POTIONS.register("long_radiation_sickness",
            () -> radiationSickness(90 * SECOND, 0));
    public static final Holder<Potion> STRONG_RADIATION_SICKNESS = POTIONS.register("strong_radiation_sickness",
            () -> radiationSickness(22 * SECOND, 1));

    public static final Holder<Potion> BLUEBIRD = POTIONS.register("bluebird", () -> bluebird(4 * SECOND, 2));
    public static final Holder<Potion> LONG_BLUEBIRD = POTIONS.register("long_bluebird", () -> bluebird(8 * SECOND, 2));
    public static final Holder<Potion> STRONG_BLUEBIRD = POTIONS.register("strong_bluebird", () -> bluebird(4 * SECOND, 4));

    private static Potion radiationSickness(int duration, int amplifier) {
        return new Potion("radiation_sickness",
                new MobEffectInstance(CNEffects.RADIATION_SICKNESS, duration, amplifier),
                new MobEffectInstance(MobEffects.CONFUSION, duration));
    }

    private static Potion bluebird(int duration, int levitationAmplifier) {
        return new Potion("bluebird",
                new MobEffectInstance(MobEffects.LEVITATION, duration, levitationAmplifier),
                new MobEffectInstance(CNEffects.BLUE_GLOW, duration));
    }

    @SubscribeEvent
    static void registerBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        addVariants(builder, CNItems.RAW_URANIUM.get(), RADIATION_SICKNESS, LONG_RADIATION_SICKNESS, STRONG_RADIATION_SICKNESS);
        addVariants(builder, CNItems.XENON_CRYSTALS.get(), BLUEBIRD, LONG_BLUEBIRD, STRONG_BLUEBIRD);
    }

    private static void addVariants(PotionBrewing.Builder builder, Item reagent,
                                    Holder<Potion> base, Holder<Potion> extended, Holder<Potion> strong) {
        builder.addMix(Potions.AWKWARD, reagent, base);
        builder.addMix(base, Items.REDSTONE, extended);
        builder.addMix(base, Items.GLOWSTONE_DUST, strong);
    }

    private CNPotions() {}
}
