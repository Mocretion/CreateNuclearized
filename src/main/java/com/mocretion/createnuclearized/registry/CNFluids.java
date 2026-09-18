package com.mocretion.createnuclearized.registry;

import static com.mocretion.createnuclearized.registry.CNRegistrate.REGISTRATE;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.fluid.DepletedSaltBucketItem;
import com.mocretion.createnuclearized.content.fluid.DepletedSaltFluidType;
import com.mocretion.createnuclearized.content.fluid.PressurizedSteamFluid;
import com.mocretion.createnuclearized.content.fluid.PressurizedSteamLiquidBlock;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class CNFluids {

    private static final ResourceLocation LAVA_STILL = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static final ResourceLocation LAVA_FLOW = ResourceLocation.withDefaultNamespace("block/lava_flow");
    private static final ResourceLocation LAVA_BUCKET = ResourceLocation.withDefaultNamespace("item/lava_bucket");
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_BUCKET = ResourceLocation.withDefaultNamespace("item/water_bucket");

    // Uranium fuel salt pumped into the reactor (the uranium fuel input) Placeholder water textures
    public static final FluidEntry<BaseFlowingFluid.Flowing> URANIUM_SALT = REGISTRATE
            .fluid("uranium_salt", WATER_STILL, WATER_FLOW)
            .lang("Uranium Salt")
            // Source has to be declared before bucket() or the builder throws
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Uranium Salt")
                .build()
            .bucket()
                .lang("Uranium Salt Bucket")
                .model((c, p) -> p.generated(c, WATER_BUCKET))
                .build()
            .register();

    // Hot spent salt a running reactor produces from uranium salt Placeholder lava textures
    // Custom fluid type + bucket so a filled/spouted bucket keeps the salts SaltStats (see DepletedSaltFluidType/DepletedSaltBucketItem)
    // Bucket is kept out of the default tab; CreateNuclearized re adds it with base stats
    public static final FluidEntry<BaseFlowingFluid.Flowing> DEPLETED_URANIUM_SALT = REGISTRATE
            .fluid("depleted_uranium_salt", LAVA_STILL, LAVA_FLOW, DepletedSaltFluidType::new)
            .lang("Depleted Uranium Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Depleted Uranium Salt")
                .build()
            .bucket(DepletedSaltBucketItem::new)
                .lang("Depleted Uranium Salt Bucket")
                .model((c, p) -> p.generated(c, LAVA_BUCKET))
                .removeTab(CreateNuclearized.MAIN_TAB_KEY)
                .build()
            .register();

    // Plutonium fuel salt: an alternative reactor input with its own base stats Placeholder water
    public static final FluidEntry<BaseFlowingFluid.Flowing> PLUTONIUM_SALT = REGISTRATE
            .fluid("plutonium_salt", WATER_STILL, WATER_FLOW)
            .lang("Plutonium Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Plutonium Salt")
                .build()
            .bucket()
                .lang("Plutonium Salt Bucket")
                .model((c, p) -> p.generated(c, WATER_BUCKET))
                .build()
            .register();

    // Hot spent salt a running reactor produces from plutonium salt Placeholder lava textures
    public static final FluidEntry<BaseFlowingFluid.Flowing> DEPLETED_PLUTONIUM_SALT = REGISTRATE
            .fluid("depleted_plutonium_salt", LAVA_STILL, LAVA_FLOW, DepletedSaltFluidType::new)
            .lang("Depleted Plutonium Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Depleted Plutonium Salt")
                .build()
            .bucket(DepletedSaltBucketItem::new)
                .lang("Depleted Plutonium Salt Bucket")
                .model((c, p) -> p.generated(c, LAVA_BUCKET))
                .removeTab(CreateNuclearized.MAIN_TAB_KEY)
                .build()
            .register();

    // Thorium uranium fuel salt: a third reactor input Placeholder water textures
    public static final FluidEntry<BaseFlowingFluid.Flowing> THORIUM_URANIUM_SALT = REGISTRATE
            .fluid("thorium_uranium_salt", WATER_STILL, WATER_FLOW)
            .lang("Thorium-Uranium Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Thorium-Uranium Salt")
                .build()
            .bucket()
                .lang("Thorium-Uranium Salt Bucket")
                .model((c, p) -> p.generated(c, WATER_BUCKET))
                .build()
            .register();

    // Hot spent salt a running reactor produces from thorium uranium salt Placeholder lava
    public static final FluidEntry<BaseFlowingFluid.Flowing> DEPLETED_THORIUM_URANIUM_SALT = REGISTRATE
            .fluid("depleted_thorium_uranium_salt", LAVA_STILL, LAVA_FLOW, DepletedSaltFluidType::new)
            .lang("Depleted Thorium-Uranium Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Depleted Thorium-Uranium Salt")
                .build()
            .bucket(DepletedSaltBucketItem::new)
                .lang("Depleted Thorium-Uranium Salt Bucket")
                .model((c, p) -> p.generated(c, LAVA_BUCKET))
                .removeTab(CreateNuclearized.MAIN_TAB_KEY)
                .build()
            .register();

    // Base carrier salt (no fuel) Plain fluid for the reprocessing chain; no reactor role yet
    public static final FluidEntry<BaseFlowingFluid.Flowing> SALT = REGISTRATE
            .fluid("salt", WATER_STILL, WATER_FLOW)
            .lang("Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Salt")
                .build()
            .bucket()
                .lang("Salt Bucket")
                .model((c, p) -> p.generated(c, WATER_BUCKET))
                .build()
            .register();

    // Molten (fuel free) carrier salt Plain fluid for the reprocessing chain; no reactor role yet
    public static final FluidEntry<BaseFlowingFluid.Flowing> MOLTEN_SALT = REGISTRATE
            .fluid("molten_salt", LAVA_STILL, LAVA_FLOW)
            .lang("Molten Salt")
            .source(BaseFlowingFluid.Source::new)
            .block()
                .lang("Molten Salt")
                .build()
            .bucket()
                .lang("Molten Salt Bucket")
                .model((c, p) -> p.generated(c, LAVA_BUCKET))
                .build()
            .register();

    public static final FluidEntry<PressurizedSteamFluid.Flowing> PRESSURIZED_STEAM = REGISTRATE
            .fluid("pressurized_steam",
                    ResourceLocation.withDefaultNamespace("block/water_still"),
                    ResourceLocation.withDefaultNamespace("block/water_flow"),
                    PressurizedSteamFluid.Flowing::new)
            .lang("Pressurized Steam")
            // Gas: rises, and entities pass right through it (no swim/drown/push)
            .properties(p -> p.density(-500).temperature(400).viscosity(200)
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .motionScale(0.0))
            // Rising gas source; must exist before bucket()
            .source(PressurizedSteamFluid.Source::new)
            // Liquid block whose source self removes after ~2 seconds
            .block(PressurizedSteamLiquidBlock::new)
                .build()
            // Bucket with a placeholder water bucket texture
            .bucket()
                .model((c, p) -> p.generated(c, ResourceLocation.withDefaultNamespace("item/water_bucket")))
                .build()
            .register();

    private CNFluids() {}

    // Source fluid accessors, for building FluidStacks and tank validators The inputs go into the reactor, the depleted forms come out

    public static Fluid uraniumSalt() {
        return URANIUM_SALT.getSource();
    }

    public static Fluid depletedUraniumSalt() {
        return DEPLETED_URANIUM_SALT.getSource();
    }

    public static Fluid plutoniumSalt() {
        return PLUTONIUM_SALT.getSource();
    }

    public static Fluid depletedPlutoniumSalt() {
        return DEPLETED_PLUTONIUM_SALT.getSource();
    }

    public static Fluid thoriumUraniumSalt() {
        return THORIUM_URANIUM_SALT.getSource();
    }

    public static Fluid depletedThoriumUraniumSalt() {
        return DEPLETED_THORIUM_URANIUM_SALT.getSource();
    }

    public static Fluid pressurizedSteam() {
        return PRESSURIZED_STEAM.getSource();
    }

    /** No op that forces this class to load so the fields above initialize */
    public static void register() {}
}
