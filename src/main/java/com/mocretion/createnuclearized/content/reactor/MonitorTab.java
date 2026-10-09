package com.mocretion.createnuclearized.content.reactor;

import java.util.Locale;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public enum MonitorTab {

    INFO("Reactor Info") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
            int line = y;
            line = row(g, font, x, line, "Fissile Buffer",
                    menu.getFissileBuffered() + " / " + menu.getFissileCapacity() + " mB");
            line = row(g, font, x, line, "Spent Buffer",
                    menu.getSpentBuffered() + " / " + menu.getSpentCapacity() + " mB");
            line = row(g, font, x, line, "Channels",
                    menu.getActiveChannels() + " / " + menu.getTotalChannels());
            line = row(g, font, x, line, "Max Flow", formatRate(menu.getTotalRate()) + " mB/t");
            row(g, font, x, line, "Runtime", ReactorMonitorScreen.formatDuration(menu.getRunSeconds()));
        }
    },

    STATS("Output Stats") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
            SaltStats s = menu.getOutputStats();
            ReactorFuel fuel = menu.getFuel();
            int line = y;
            line = row(g, font, x, line, "Heat", s.heat() + "°");
            line = elementRow(g, font, x, line, fuel.unconsumed(), s);
            line = elementRow(g, font, x, line, fuel.bred(), s);
            statRow(g, font, x, line, "Off-gas", s.offGas());
        }
    };

    private static final int LINE_HEIGHT = 11;
    private static final int TEXT_COLOR = 0x404040;

    private final String label;

    MonitorTab(String label) {
        this.label = label;
    }

    public Component title() {
        return Component.literal(label);
    }

    public abstract void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y);

    protected static int row(GuiGraphics g, Font font, int x, int y, String label, String value) {
        g.drawString(font, label + ": " + value, x, y, TEXT_COLOR, false);
        return y + LINE_HEIGHT;
    }

    protected static int statRow(GuiGraphics g, Font font, int x, int y, String label, int percent) {
        if (percent == 0) return y;
        return row(g, font, x, y, label, percent + "%");
    }

    protected static int elementRow(GuiGraphics g, Font font, int x, int y, ReactorFuel.Element element, SaltStats s) {
        if (element == null) return y;
        String label = element.name().charAt(0) + element.name().substring(1).toLowerCase(Locale.ROOT);
        return statRow(g, font, x, y, label, s.amount(element));
    }

    protected static String formatRate(double rate) {
        String s = String.format(Locale.ROOT, "%.3f", rate);
        if (s.contains("."))
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
