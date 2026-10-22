package com.mocretion.createnuclearized.content.boiler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class IndustrialHeaterRenderer extends SafeBlockEntityRenderer<IndustrialHeaterBlockEntity> {

    private static final float BUCKET_OFFSET = 0.28f;
    private static final float BUCKET_SPREAD_ACTIVE = 0.08f;
    private static final float BUCKET_HEIGHT = 0.4f;
    private static final float BUCKET_RISE_ACTIVE = 0.18f;
    private static final float BUCKET_SCALE = 0.55f;
    private static final float BUCKET_YAW = 90f;
    private static final float FACE_PLAYER_TILT = 45f;

    public IndustrialHeaterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(IndustrialHeaterBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource bufferSource, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockState state = be.getBlockState();

        boolean active = be.isActive();
        float animation = be.getHeadAnimation(partialTicks) * 0.175f;
        float horizontalAngle = AngleHelper.rad(be.getHeadAngle(partialTicks));
        HeatLevel heat = IndustrialHeaterBlock.heatLevel(active);
        BlazeBurnerRenderer.renderShared(ms, null, bufferSource,
                level, state, heat, animation, horizontalAngle,
                active, false, IndustrialHeaterPartials.HAZMAT_HELMET, be.hashCode());

        renderBucket(ms, bufferSource, level, light, overlay, horizontalAngle, -1f, active, be.getBufferDisplay(0));
        renderBucket(ms, bufferSource, level, light, overlay, horizontalAngle, 1f, active, be.getBufferDisplay(1));
    }

    private static void renderBucket(PoseStack ms, MultiBufferSource bufferSource, Level level, int light, int overlay,
                                     float blazeAngle, float side, boolean active, FluidStack fluid) {
        ItemStack stack = fluid.isEmpty() ? new ItemStack(Items.BUCKET) : new ItemStack(fluid.getFluid().getBucket());
        if (stack.isEmpty()) return;

        float height = active ? BUCKET_HEIGHT + BUCKET_RISE_ACTIVE : BUCKET_HEIGHT;
        float offset = active ? BUCKET_OFFSET + BUCKET_SPREAD_ACTIVE : BUCKET_OFFSET;

        ms.pushPose();
        ms.translate(0.5f, height, 0.5f);
        ms.mulPose(Axis.YP.rotation(blazeAngle));
        ms.translate(side * offset, 0, 0);
        ms.mulPose(Axis.YP.rotationDegrees(side * (BUCKET_YAW - FACE_PLAYER_TILT)));
        ms.scale(BUCKET_SCALE, BUCKET_SCALE, BUCKET_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                light, overlay, ms, bufferSource, level, 0);
        ms.popPose();
    }
}
