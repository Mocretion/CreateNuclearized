package com.mocretion.createnuclearized.content.menu;

import com.mocretion.createnuclearized.CreateNuclearized;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

public class CNIcons {

    public static final ResourceLocation ATLAS =
            ResourceLocation.fromNamespaceAndPath(CreateNuclearized.MODID, "textures/gui/icons.png");

    public static final AllIcons BLOCK_INPUT = new Icon(0, 0);
    public static final AllIcons SNOW_FLAKE = new Icon(1, 0);
    public static final AllIcons FIRE = new Icon(2, 0);

    private static final class Icon extends AllIcons {

        private static final int ATLAS_SIZE = 256;
        private static final int FULL_BRIGHT = 15728880;

        private final int u;
        private final int v;

        Icon(int column, int row) {
            super(column, row);
            this.u = column * 16;
            this.v = row * 16;
        }

        @Override
        public void bind() {
            RenderSystem.setShaderTexture(0, ATLAS);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y) {
            graphics.blit(ATLAS, x, y, 0, u, v, 16, 16, ATLAS_SIZE, ATLAS_SIZE);
        }

        @Override
        public void render(PoseStack ms, MultiBufferSource buffer, int color) {
            VertexConsumer consumer = buffer.getBuffer(RenderType.text(ATLAS));
            Matrix4f matrix = ms.last().pose();
            Color tint = new Color(color);
            float u0 = u / (float) ATLAS_SIZE;
            float u1 = (u + 16) / (float) ATLAS_SIZE;
            float v0 = v / (float) ATLAS_SIZE;
            float v1 = (v + 16) / (float) ATLAS_SIZE;
            vertex(consumer, matrix, new Vec3(0, 0, 0), tint, u0, v0);
            vertex(consumer, matrix, new Vec3(0, 1, 0), tint, u0, v1);
            vertex(consumer, matrix, new Vec3(1, 1, 0), tint, u1, v1);
            vertex(consumer, matrix, new Vec3(1, 0, 0), tint, u1, v0);
        }

        private void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, Color color, float u, float v) {
            consumer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                    .setColor(color.getRed(), color.getGreen(), color.getBlue(), 255)
                    .setUv(u, v)
                    .setLight(FULL_BRIGHT);
        }
    }
}
