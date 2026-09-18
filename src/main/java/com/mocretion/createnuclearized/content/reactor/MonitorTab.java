package com.mocretion.createnuclearized.content.reactor;

import java.util.Locale;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public enum MonitorTab {

    // Live operation: whats buffered and flowing right now
    INFO("Reactor Info") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
            int line = y;
            line = row(g, font, x, line, "Fissile Buffer",
                    menu.getFissileBuffered() + " / " + menu.getFissileCapacity() + " mB");
            line = row(g, font, x, line, "Depleted Buffer",
                    menu.getDepletedBuffered() + " / " + menu.getDepletedCapacity() + " mB");
            line = row(g, font, x, line, "Channels",
                    menu.getActiveChannels() + " / " + menu.getTotalChannels());
            line = row(g, font, x, line, "Max Flow", formatRate(menu.getTotalRate()) + " mB/t");
            row(g, font, x, line, "Runtime", ReactorMonitorScreen.formatDuration(menu.getRunSeconds()));
        }
    },

    // The composition this build makes
    STATS("Output Stats") {
        @Override
        public void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y) {
            SaltStats s = menu.getOutputStats();
            int line = y;
            line = row(g, font, x, line, "Heat", s.heat() + "°");
            line = statRow(g, font, x, line, "Uranium", s.uranium());
            line = statRow(g, font, x, line, "Plutonium", s.plutonium());
            line = statRow(g, font, x, line, "Thorium", s.thorium());
            statRow(g, font, x, line, "Byproducts", s.byproducts());
        }
    };

    private static final int LINE_HEIGHT = 11;
    private static final int TEXT_COLOR = 0x404040; // matches the other reactor screens

    private final String label;

    MonitorTab(String label) {
        this.label = label;
    }

    /** Name shown as a tooltip when the tab is hovered */
    public Component title() {
        return Component.literal(label);
    }

    /**
     * Draw this tabs content
     * (x, y) is the top left of the content area relative to the window, since the screen calls this inside the label space transform
     */
    public abstract void renderContent(GuiGraphics g, Font font, ReactorMonitorMenu menu, int x, int y);

    /** Draw a "Label: value" line and return the y for the next one */
    protected static int row(GuiGraphics g, Font font, int x, int y, String label, String value) {
        g.drawString(font, label + ": " + value, x, y, TEXT_COLOR, false);
        return y + LINE_HEIGHT;
    }

    /** Like row, but a percentage stat thats skipped (no line) when zero */
    protected static int statRow(GuiGraphics g, Font font, int x, int y, String label, int percent) {
        if (percent == 0) return y;
        return row(g, font, x, y, label, percent + "%");
    }

    /** Rate to at most 3 decimals, trailing zeros dropped: "0.25", "1.5" */
    protected static String formatRate(double rate) {
        String s = String.format(Locale.ROOT, "%.3f", rate);
        if (s.contains("."))
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
