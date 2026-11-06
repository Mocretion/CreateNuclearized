package com.mocretion.createnuclearized.content.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiAnimation {

    public static final int WHITE = 0xFFFFFFFF;

    private final ResourceLocation texture;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final int millisPerFrame;

    public GuiAnimation(ResourceLocation texture, int frameWidth, int frameHeight, int frameCount, int millisPerFrame) {
        this.texture = texture;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.millisPerFrame = millisPerFrame;
    }

    public int frame() {
        return (int) ((System.currentTimeMillis() / millisPerFrame) % frameCount);
    }

    public void render(GuiGraphics graphics, int x, int y, int argb) {
        render(graphics, x, y, frameWidth, frameHeight, argb);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        if (a == 0f) a = 1f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        graphics.setColor(r, g, b, a);
        int sheetHeight = frameHeight * frameCount;
        graphics.blit(texture, x, y, width, height, 0F, frame() * frameHeight,
                frameWidth, frameHeight, frameWidth, sheetHeight);
        graphics.setColor(1F, 1F, 1F, 1F);
    }
}
