package com.mocretion.createnuclearized.content.reactor;

import javax.annotation.Nullable;

import com.mocretion.createnuclearized.CreateNuclearized;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public enum MonitorTab {

    INFO("info") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
            int line = y;
            line = row(g, font, x, line, label("fissile_buffer"),
                    menu.getFissileBuffered() + " / " + menu.getFissileCapacity() + " mB");
            line = row(g, font, x, line, label("spent_buffer"),
                    menu.getSpentBuffered() + " / " + menu.getSpentCapacity() + " mB");
            line = row(g, font, x, line, label("channels"),
                    menu.getActiveChannels() + " / " + menu.getTotalChannels());
            line = row(g, font, x, line, label("max_flow"), RateFormat.format(menu.getTotalRate()) + " mB/t");
            row(g, font, x, line, label("runtime"), ReactorMonitorScreen.formatDuration(menu.getRunSeconds()).getString());
        }
    },

    STATS("stats") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
            SaltStats s = menu.getOutputStats();
            ReactorFuel fuel = menu.getFuel();
            int line = y;
            line = row(g, font, x, line, label("heat"), s.heat() + "°");
            line = elementRow(g, font, x, line, fuel.unconsumed(), s);
            if (fuel.bred() != fuel.unconsumed())
                line = elementRow(g, font, x, line, fuel.bred(), s);
            statRow(g, font, x, line, label("off_gas"), s.offGas());
        }
    },

    INTERIOR("interior") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
        }
    };

    private static final String KEY = CreateNuclearized.MODID + ".gui.reactor_monitor.";
    private static final int LINE_HEIGHT = 11;
    private static final int TEXT_COLOR = 0x404040;

    private final String id;

    MonitorTab(String id) {
        this.id = id;
    }

    public Component title() {
        return Component.translatable(KEY + "tab." + id);
    }

    public abstract void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y);

    protected static Component label(String key) {
        return Component.translatable(KEY + key);
    }

    protected static int row(GuiGraphics g, Font font, int x, int y, Component label, String value) {
        g.drawString(font, Component.translatable(KEY + "row", label, value), x, y, TEXT_COLOR, false);
        return y + LINE_HEIGHT;
    }

    protected static int statRow(GuiGraphics g, Font font, int x, int y, Component label, int percent) {
        if (percent == 0) return y;
        return row(g, font, x, y, label, percent + "%");
    }

    protected static int elementRow(GuiGraphics g, Font font, int x, int y, @Nullable SaltElement element, SaltStats s) {
        if (element == null) return y;
        return statRow(g, font, x, y, element.displayName(), s.amount(element));
    }
}
