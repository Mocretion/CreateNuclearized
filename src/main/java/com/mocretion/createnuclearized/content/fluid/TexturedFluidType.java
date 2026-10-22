package com.mocretion.createnuclearized.content.fluid;

import java.util.function.Consumer;

import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import org.joml.Vector3f;

public class TexturedFluidType extends FluidType {

    private static final float FOG_START = 0.0f;
    private static final float FOG_END = 0.8f;

    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final int tint;
    private int radiationLevel;
    private boolean burns;

    public TexturedFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture, int tint) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.tint = tint;
    }

    public TexturedFluidType radiation(int radiationLevel, boolean burns) {
        this.radiationLevel = radiationLevel;
        this.burns = burns;
        return this;
    }

    public int getRadiationLevel() {
        return radiationLevel;
    }

    public boolean burns() {
        return burns;
    }

    @Override
    public int getTemperature(FluidStack stack) {
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        return stats != null ? stats.heat() : super.getTemperature(stack);
    }

    @Override
    public ItemStack getBucket(FluidStack stack) {
        ItemStack bucket = super.getBucket(stack);
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null)
            bucket.set(CNDataComponents.SALT_STATS.get(), stats);
        return bucket;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public int getTintColor() {
                return tint;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                float r = ((tint >> 16) & 0xFF) / 255f;
                float g = ((tint >> 8) & 0xFF) / 255f;
                float b = (tint & 0xFF) / 255f;
                return new Vector3f(r, g, b);
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
                                        float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(FOG_START);
                RenderSystem.setShaderFogEnd(FOG_END);
            }
        });
    }
}
