package com.mocretion.createnuclearized.compat.jei;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mocretion.createnuclearized.content.reactor.ReactorFuel;
import com.mocretion.createnuclearized.content.reactor.SaltStats;
import com.mocretion.createnuclearized.content.reactor.SaltStatsTooltip;
import com.mocretion.createnuclearized.content.boiler.GasEffect;
import com.mocretion.createnuclearized.content.boiler.GasHazards;
import com.mocretion.createnuclearized.content.boiler.GasParticles;
import com.mocretion.createnuclearized.registry.CNDataComponents;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

public abstract class CNRecipeCategory<T extends Recipe<? extends RecipeInput>> extends CreateRecipeCategory<T> {

    protected static final int TEXT_COLOR = 0xFF525252;
    protected static final int WHITE = 0xFFFFFFFF;
    protected static final int HAZARD_X = 5;

    private static final ResourceLocation INFO_BORDER =
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "textures/gui/jei/jei_info_border.png");
    private static final int INFO_BORDER_W = 76;
    private static final int INFO_BORDER_H = 45;

    private static final ResourceLocation HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ItemStack LAVA_BUCKET = new ItemStack(Items.LAVA_BUCKET);
    private static final int HAZARD_SIZE = 11;
    private static final int HAZARD_STEP = 13;
    private static final int HEART_SIZE = HAZARD_SIZE - 2;

    protected final IGuiHelper guiHelper;
    protected final IDrawable arrow;

    protected CNRecipeCategory(Info<T> info, IGuiHelper guiHelper) {
        super(info);
        this.guiHelper = guiHelper;
        this.arrow = guiHelper.getRecipeArrow();
    }

    protected IRecipeSlotBuilder fluidSlot(IRecipeLayoutBuilder layout, RecipeIngredientRole role, int x, int y, FluidStack stack) {
        return layout.addSlot(role, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredient(NeoForgeTypes.FLUID_STACK, stack)
                .setFluidRenderer(Math.max(stack.getAmount(), 1), false, 16, 16);
    }

    protected IRecipeSlotBuilder fluidSlot(IRecipeLayoutBuilder layout, RecipeIngredientRole role, int x, int y,
            FluidIngredient ingredient, int amount) {
        List<FluidStack> stacks = new ArrayList<>();
        for (FluidStack stack : ingredient.getStacks())
            if (stack.getFluid().defaultFluidState().isSource())
                stacks.add(stack.copyWithAmount(amount));
        if (stacks.isEmpty())
            for (FluidStack stack : ingredient.getStacks())
                stacks.add(stack.copyWithAmount(amount));
        return layout.addSlot(role, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(NeoForgeTypes.FLUID_STACK, stacks)
                .setFluidRenderer(Math.max(amount, 1), false, 16, 16);
    }

    protected IRecipeSlotBuilder fluidSlot(IRecipeLayoutBuilder layout, RecipeIngredientRole role, int x, int y, List<FluidStack> stacks) {
        int amount = stacks.isEmpty() ? 1 : Math.max(stacks.get(0).getAmount(), 1);
        return layout.addSlot(role, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .addIngredients(NeoForgeTypes.FLUID_STACK, stacks)
                .setFluidRenderer(amount, false, 16, 16);
    }

    protected IRecipeSlotBuilder itemSlot(IRecipeLayoutBuilder layout, RecipeIngredientRole role, int x, int y, ItemStack stack) {
        return layout.addSlot(role, x, y)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStack(stack);
    }

    protected IRecipeSlotBuilder itemSlot(IRecipeLayoutBuilder layout, RecipeIngredientRole role, int x, int y, ProcessingOutput output) {
        return layout.addSlot(role, x, y)
                .setBackground(getRenderedSlot(output), -1, -1)
                .addItemStack(output.getStack())
                .addRichTooltipCallback(addStochasticTooltip(output));
    }

    protected void infoBorder(GuiGraphics graphics, int x, int y) {
        graphics.blit(INFO_BORDER, x, y, 0, 0, INFO_BORDER_W, INFO_BORDER_H, INFO_BORDER_W, INFO_BORDER_H);
    }

    protected void drawHazards(GuiGraphics graphics, GasHazards hazards, int x, int y) {
        int cx = x;
        for (GasEffect effect : hazards.effects()) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect.effect());
            graphics.blit(cx, y, 0, HAZARD_SIZE, HAZARD_SIZE, sprite);
            cx += HAZARD_STEP;
        }
        if (hazards.damage().isPresent()) {
            graphics.blitSprite(HEART_CONTAINER, cx + 1, y + 1, HEART_SIZE, HEART_SIZE);
            graphics.blitSprite(HEART_HALF, cx + 1, y + 1, HEART_SIZE, HEART_SIZE);
            cx += HAZARD_STEP;
        }
        if (hazards.ignite() > 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(cx, y, 0);
            graphics.pose().scale(HAZARD_SIZE / 16f, HAZARD_SIZE / 16f, 1);
            graphics.renderItem(LAVA_BUCKET, 0, 0);
            graphics.pose().popPose();
        }
    }

    protected List<Component> hazardTooltip(GasHazards hazards, int x, int y, double mouseX, double mouseY) {
        int cx = x;
        for (GasEffect effect : hazards.effects()) {
            if (overIcon(cx, y, mouseX, mouseY)) return effectTooltip(effect);
            cx += HAZARD_STEP;
        }
        if (hazards.damage().isPresent()) {
            if (overIcon(cx, y, mouseX, mouseY))
                return List.of(Component.translatable("createnuclearized.jei.hazard_damage", formatFloat(hazards.damage().get().amount())));
            cx += HAZARD_STEP;
        }
        if (hazards.ignite() > 0 && overIcon(cx, y, mouseX, mouseY))
            return List.of(Component.translatable("createnuclearized.jei.hazard_ignite", formatFloat(hazards.ignite())));
        return List.of();
    }

    private static boolean overIcon(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + HAZARD_SIZE && mouseY >= y && mouseY < y + HAZARD_SIZE;
    }

    private static List<Component> effectTooltip(GasEffect effect) {
        MobEffect mob = effect.effect().value();
        MutableComponent name = Component.translatable(mob.getDescriptionId());
        if (effect.amplifier() > 0)
            name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + effect.amplifier()));
        MutableComponent line = Component.translatable("potion.withDuration", name, MobEffectUtil.formatDuration(effect.instance(), 1f, 20f));
        ChatFormatting color = mob.getCategory() == MobEffectCategory.HARMFUL ? ChatFormatting.RED : ChatFormatting.BLUE;
        return List.of(line.withStyle(color));
    }

    protected static String formatFloat(float value) {
        if (value == Math.rint(value)) return Integer.toString((int) value);
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    protected void line(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, TEXT_COLOR, false);
    }

    protected void line(GuiGraphics graphics, Component text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    protected void risingParticles(GuiGraphics graphics, GasParticles particles, int centerX, int topY, int bottomY) {
        int tint = particleTint(particles.particle());
        float time = AnimationTickHolder.getRenderTime();
        int count = 6;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 260);
        for (int k = 0; k < count; k++) {
            float phase = ((time * 0.02f) + k / (float) count) % 1f;
            int py = (int) (bottomY - phase * (bottomY - topY));
            int px = centerX + (int) (Mth.sin(time * 0.1f + k * 1.7f) * 5);
            int alpha = (int) (Math.max(0f, 1f - phase) * 190);
            graphics.fill(px, py, px + 2, py + 2, (alpha << 24) | (tint & 0xFFFFFF));
        }
        graphics.pose().popPose();
    }

    private static int particleTint(ParticleOptions particle) {
        if (particle.getType() == ParticleTypes.SMOKE || particle.getType() == ParticleTypes.LARGE_SMOKE)
            return 0x4A4A4A;
        return 0xE6E6E6;
    }

    protected void renderMirroredX(GuiGraphics graphics, AllGuiTextures texture, int x, int y) {
        int w = texture.getWidth();
        int h = texture.getHeight();
        graphics.blit(texture.getLocation(), x, y, w, h, texture.getStartX() + w, texture.getStartY(), -w, h, 256, 256);
    }

    protected void renderMirroredY(GuiGraphics graphics, AllGuiTextures texture, int x, int y) {
        int w = texture.getWidth();
        int h = texture.getHeight();
        graphics.blit(texture.getLocation(), x, y, w, h, texture.getStartX(), texture.getStartY() + h, w, -h, 256, 256);
    }

    protected void saltTooltip(IRecipeSlotBuilder slot, SaltStats stats, @Nullable ReactorFuel fuel) {
        slot.addTooltipCallback((view, tooltip) -> SaltStatsTooltip.append(tooltip, stats, fuel));
    }

    protected void saltTooltip(IRecipeSlotBuilder slot, FluidStack stack, @Nullable ReactorFuel fuel) {
        SaltStats stats = stack.get(CNDataComponents.SALT_STATS.get());
        if (stats != null) saltTooltip(slot, stats, fuel);
    }

    protected void hideFluidAmount(IRecipeSlotBuilder slot) {
        slot.addTooltipCallback((view, tooltip) -> tooltip.removeIf(c ->
                c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && tc.getKey().startsWith("jei.tooltip.liquid.amount")));
    }

    protected void saltTooltip(IRecipeSlotBuilder slot) {
        slot.addTooltipCallback((view, tooltip) -> view.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK).ifPresent(shown -> {
            SaltStats stats = shown.get(CNDataComponents.SALT_STATS.get());
            if (stats != null) SaltStatsTooltip.append(tooltip, stats, ReactorFuel.forOutput(shown.getFluid()));
        }));
    }
}
