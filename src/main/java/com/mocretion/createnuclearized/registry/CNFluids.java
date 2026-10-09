package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.fluid.PressurizedSteamFluid;
import com.mocretion.createnuclearized.content.fluid.PressurizedSteamLiquidBlock;
import com.mocretion.createnuclearized.content.fluid.SpentSaltBucketItem;
import com.mocretion.createnuclearized.content.fluid.SpentSaltLiquidBlock;
import com.mocretion.createnuclearized.content.fluid.TexturedFluidType;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CNFluids {

    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_BUCKET = ResourceLocation.withDefaultNamespace("item/water_bucket");
    private static final ResourceLocation LIQUID_STILL = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/liquid_still");
    private static final ResourceLocation LIQUID_FLOW = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/liquid_flow");
    private static final ResourceLocation LIQUID_HOT_STILL = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/liquid_hot_still");
    private static final ResourceLocation LIQUID_HOT_FLOW = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "block/liquid_hot_flow");

    public static final TagKey<Fluid> SPENT_SALTS =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "spent_salts"));

    private static final int HOT_THORIUM_URANIUM_TEMPERATURE = 1400;
    private static final int HOT_URANIUM_TEMPERATURE = 2489;
    private static final int HOT_PLUTONIUM_TEMPERATURE = 4480;

    private static final int LIQUID_URANIUM_TINT = 0xFF637158;
    private static final int LIQUID_PLUTONIUM_TINT = 0xFF585871;
    private static final int LIQUID_THORIUM_TINT = 0xFF292828;
    private static final int LIQUID_THORIUM_URANIUM_TINT = 0xFF58716D;
    private static final int LIQUID_AMERICIUM_TINT = 0xFF715858;

    private static final int ENRICHED_LIQUID_URANIUM_TINT = 0xFF78886D;

    private static final int URANIUM_SALT_TINT = 0xFFFFFFE8;
    private static final int LIQUID_PLUTONIUM_SALT_TINT = 0xFFC5C5FA;
    private static final int THORIUM_URANIUM_SALT_TINT = 0xFFC5FAF0;

    private static final int SPENT_PLUTONIUM_SALT_TINT = 0xFFE8E0FF;
    private static final int SPENT_THORIUM_URANIUM_SALT_TINT = 0xFFDBFFFF;
    private static final int SPENT_URANIUM_SALT_TINT = 0xFFE9FAD0;
    private static final int MOLTEN_SALT_TINT = 0xFFF4F4F4;

    private static final int THORIUM_RADIATION = 1;
    private static final int URANIUM_RADIATION = 2;
    private static final int PLUTONIUM_RADIATION = 3;
    private static final int AMERICIUM_RADIATION = 4;

    public static final FluidEntry<BaseFlowingFluid.Flowing> URANIUM_SALT =
            salt("uranium_salt", "Uranium Salt", URANIUM_SALT_TINT, URANIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> SPENT_URANIUM_SALT =
            spentSalt("spent_uranium_salt", "Spent Uranium Salt",
                    SPENT_URANIUM_SALT_TINT, HOT_URANIUM_TEMPERATURE, URANIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> PLUTONIUM_SALT =
            salt("plutonium_salt", "Plutonium Salt", LIQUID_PLUTONIUM_SALT_TINT, PLUTONIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> SPENT_PLUTONIUM_SALT =
            spentSalt("spent_plutonium_salt", "Spent Plutonium Salt",
                    SPENT_PLUTONIUM_SALT_TINT, HOT_PLUTONIUM_TEMPERATURE, PLUTONIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> THORIUM_URANIUM_SALT =
            salt("thorium_uranium_salt", "Thorium-Uranium Salt", THORIUM_URANIUM_SALT_TINT, THORIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> SPENT_THORIUM_URANIUM_SALT =
            spentSalt("spent_thorium_uranium_salt", "Spent Thorium-Uranium Salt",
                    SPENT_THORIUM_URANIUM_SALT_TINT, HOT_THORIUM_URANIUM_TEMPERATURE, THORIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> MOLTEN_SALT =
            withBucket(tintedFluid("molten_salt", MOLTEN_SALT_TINT, 0), "molten_salt", "Molten Salt");

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_URANIUM =
            liquidMetal("liquid_uranium", "Liquid Uranium", LIQUID_URANIUM_TINT, URANIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_PLUTONIUM =
            liquidMetal("liquid_plutonium", "Liquid Plutonium", LIQUID_PLUTONIUM_TINT, PLUTONIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_THORIUM =
            liquidMetal("liquid_thorium", "Liquid Thorium", LIQUID_THORIUM_TINT, THORIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_AMERICIUM =
            liquidMetal("liquid_americium", "Liquid Americium", LIQUID_AMERICIUM_TINT, AMERICIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_THORIUM_URANIUM =
            liquidMetal("liquid_thorium_uranium", "Liquid Thorium-Uranium", LIQUID_THORIUM_URANIUM_TINT, THORIUM_RADIATION);

    public static final FluidEntry<BaseFlowingFluid.Flowing> ENRICHED_LIQUID_URANIUM =
            liquidMetal("enriched_liquid_uranium", "Enriched Liquid Uranium", ENRICHED_LIQUID_URANIUM_TINT, URANIUM_RADIATION);

    public static final FluidEntry<PressurizedSteamFluid.Flowing> PRESSURIZED_STEAM = REGISTRATE
            .fluid("pressurized_steam", WATER_STILL, WATER_FLOW, PressurizedSteamFluid.Flowing::new)
            .lang("Pressurized Steam")
            .properties(p -> p.density(-500).temperature(400).viscosity(200)
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .motionScale(0.0))
            .source(PressurizedSteamFluid.Source::new)
            .block(PressurizedSteamLiquidBlock::new)
                .build()
            .bucket()
                .model((c, p) -> p.generated(c,
                        ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "item/pressurized_steam_bucket")))
                .build()
            .register();

    public static final FluidEntry<PressurizedSteamFluid.Flowing> FISSION_GAS = REGISTRATE
            .fluid("fission_gas", WATER_STILL, WATER_FLOW, PressurizedSteamFluid.Flowing::new)
            .lang("Fission Gas")
            .properties(p -> p.density(-500).viscosity(200)
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .motionScale(0.0))
            .source(PressurizedSteamFluid.Source::new)
            .block(PressurizedSteamLiquidBlock::new)
                .build()
            .bucket()
                .model((c, p) -> p.generated(c, WATER_BUCKET))
                .build()
            .register();

    private CNFluids() {}

    private static FluidEntry<BaseFlowingFluid.Flowing> salt(String name, String lang, int tint, int radiation) {
        return withBucket(tintedFluid(name, tint, radiation), name, lang);
    }

    private static FluidEntry<BaseFlowingFluid.Flowing> liquidMetal(String name, String lang, int tint, int radiation) {
        return withBucket(tintedFluid(name, tint, radiation)
                .fluidProperties(p -> p.tickRate(25).levelDecreasePerBlock(2).slopeFindDistance(2)), name, lang);
    }

    private static FluidEntry<BaseFlowingFluid.Flowing> spentSalt(String name, String lang, int tint, int temperature, int radiation) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "item/" + name + "_bucket");
        return hotTintedFluid(name, tint, temperature, radiation)
                .tag(SPENT_SALTS)
                .lang(lang)
                .source(BaseFlowingFluid.Source::new)
                .block(SpentSaltLiquidBlock::new)
                    .lang(lang)
                    .build()
                .bucket(SpentSaltBucketItem::new)
                    .lang(lang + " Bucket")
                    .model((c, p) -> p.generated(c, texture))
                    .build()
                .register();
    }

    private static FluidBuilder<BaseFlowingFluid.Flowing, CreateRegistrate> tintedFluid(String name, int tint, int radiation) {
        return REGISTRATE.fluid(name, LIQUID_STILL, LIQUID_FLOW,
                (props, still, flow) -> new TexturedFluidType(props, still, flow, tint).radiation(radiation, false));
    }

    private static FluidBuilder<BaseFlowingFluid.Flowing, CreateRegistrate> hotTintedFluid(String name, int tint, int temperature, int radiation) {
        return REGISTRATE.fluid(name, LIQUID_HOT_STILL, LIQUID_HOT_FLOW,
                (props, still, flow) -> new TexturedFluidType(props.temperature(temperature), still, flow, tint).radiation(radiation, true));
    }

    private static FluidEntry<BaseFlowingFluid.Flowing> withBucket(FluidBuilder<BaseFlowingFluid.Flowing, CreateRegistrate> fluid,
                                                                   String name, String lang) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "item/" + name + "_bucket");
        return fluid.lang(lang)
                .source(BaseFlowingFluid.Source::new)
                .block()
                    .lang(lang)
                    .build()
                .bucket()
                    .lang(lang + " Bucket")
                    .model((c, p) -> p.generated(c, texture))
                    .build()
                .register();
    }

    public static Fluid uraniumSalt() {
        return URANIUM_SALT.getSource();
    }

    public static Fluid spentUraniumSalt() {
        return SPENT_URANIUM_SALT.getSource();
    }

    public static Fluid plutoniumSalt() {
        return PLUTONIUM_SALT.getSource();
    }

    public static Fluid spentPlutoniumSalt() {
        return SPENT_PLUTONIUM_SALT.getSource();
    }

    public static Fluid thoriumUraniumSalt() {
        return THORIUM_URANIUM_SALT.getSource();
    }

    public static Fluid spentThoriumUraniumSalt() {
        return SPENT_THORIUM_URANIUM_SALT.getSource();
    }

    public static Fluid liquidUranium() {
        return LIQUID_URANIUM.getSource();
    }

    public static Fluid liquidPlutonium() {
        return LIQUID_PLUTONIUM.getSource();
    }

    public static Fluid liquidThorium() {
        return LIQUID_THORIUM.getSource();
    }

    public static Fluid liquidAmericium() {
        return LIQUID_AMERICIUM.getSource();
    }

    public static Fluid moltenSalt() {
        return MOLTEN_SALT.getSource();
    }

    public static Fluid liquidFor(ReactorFuel.Element element) {
        return switch (element) {
            case URANIUM -> liquidUranium();
            case PLUTONIUM -> liquidPlutonium();
            case THORIUM -> liquidThorium();
            case AMERICIUM -> liquidAmericium();
        };
    }

    public static FluidStack spentOrMolten(Fluid spent, SaltStats stats, int amount) {
        if (stats.isEmpty())
            return new FluidStack(moltenSalt().builtInRegistryHolder(), amount);
        FluidStack out = new FluidStack(spent.builtInRegistryHolder(), amount);
        out.set(CNDataComponents.SALT_STATS.get(), stats);
        return out;
    }

    public static Fluid pressurizedSteam() {
        return PRESSURIZED_STEAM.getSource();
    }

    public static void register() {}
}
