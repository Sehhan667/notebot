/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Small immediate-mode helper for the 26.3 GUI model: drawing happens in
 * {@code extractRenderState}, clicks are matched against hotspots registered during that draw.
 */
public final class UiKit {
    public static final int PANEL = 0xF0121218;
    public static final int PANEL_LIGHT = 0xFF1C1C24;
    public static final int BORDER = 0xFF3A3A46;
    public static final int TEXT = 0xFFEDEDF2;
    public static final int TEXT_DIM = 0xFF9A9AA6;
    public static final int ACCENT = 0xFF8B6FE8;
    public static final int ACCENT_DIM = 0xFF4A3E7A;
    public static final int BUTTON = 0xFF2A2A34;
    public static final int BUTTON_HOVER = 0xFF3A3A48;
    public static final int ROW_HOVER = 0x22FFFFFF;
    public static final int ROW_H = 20;

    private UiKit() {
    }

    public static void rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    public static void frame(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        // 26.3 outline() takes width/height, not absolute corners.
        g.outline(x1, y1, x2 - x1, y2 - y1, color);
    }

    public static String trim(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - 8)) + "…";
    }

    public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        g.text(font, Lang.ui(text), x, y, color);
    }

    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        g.text(font, text, x, y, color);
    }

    public static void centered(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int color) {
        Component component = Lang.ui(text);
        g.text(font, component, centerX - font.width(component) / 2, y, color);
    }

    /** A clickable rectangle registered while drawing. */
    public record Hotspot(int x1, int y1, int x2, int y2, Runnable action) {
        public boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    /** Collects hotspots for the current frame. */
    public static final class Hotspots {
        private final List<Hotspot> list = new ArrayList<>();

        public void clear() {
            list.clear();
        }

        public void add(int x1, int y1, int x2, int y2, Runnable action) {
            list.add(new Hotspot(x1, y1, x2, y2, action));
        }

        /** Runs the topmost hotspot containing the point; returns true when one fired. */
        public boolean click(double x, double y) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Hotspot hotspot = list.get(i);
                if (hotspot.contains(x, y)) {
                    hotspot.action().run();
                    return true;
                }
            }
            return false;
        }
    }

    /** Draws a button and registers its click area. */
    public static void button(GuiGraphicsExtractor g, Font font, Hotspots hotspots,
                              int x, int y, int w, int h, String label, boolean hovered, Runnable action) {
        Component text = Lang.ui(label);
        g.fill(x, y, x + w, y + h, hovered ? BUTTON_HOVER : BUTTON);
        g.outline(x, y, w, h, BORDER);
        g.text(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, TEXT);
        hotspots.add(x, y, x + w, y + h, action);
    }

    /** Draws a horizontal progress/value bar; returns nothing, caller handles input. */
    public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float value, int fillColor) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        int filled = (int) (w * Math.max(0.0f, Math.min(1.0f, value)));
        if (filled > 0) g.fill(x, y, x + filled, y + h, fillColor);
        g.outline(x, y, w, h, BORDER);
    }

    public static String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis / 1000L);
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    /** Pixel-art heart, 7x6 cells scaled up; used for the favourite marker. */
    public static void heart(GuiGraphicsExtractor g, int x, int y, int scale, int color) {
        int[] rows = {0b0110110, 0b1111111, 0b1111111, 0b0111110, 0b0011100, 0b0001000};
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < 7; col++) {
                if ((rows[row] & (1 << (6 - col))) != 0) {
                    g.fill(x + col * scale, y + row * scale,
                        x + (col + 1) * scale, y + (row + 1) * scale, color);
                }
            }
        }
    }

    /** Heart inside a button-sized box. */
    public static void heartButton(GuiGraphicsExtractor g, Hotspots hotspots, int x, int y, int w, int h,
                                   boolean favorite, boolean hovered, Runnable action) {
        g.fill(x, y, x + w, y + h, hovered ? BUTTON_HOVER : BUTTON);
        g.outline(x, y, w, h, BORDER);
        heart(g, x + (w - 14) / 2, y + (h - 12) / 2, 2, favorite ? 0xFFFF5C7A : 0xFF6A6A76);
        hotspots.add(x, y, x + w, y + h, action);
    }
}
