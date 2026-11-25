/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

import dev.notebot.NotebotMod;
import dev.notebot.config.NotebotConfig;
import dev.notebot.playback.PortableTarget;
import dev.notebot.util.InstrumentDetectMode;
import dev.notebot.util.NotebotMode;
import dev.notebot.util.OptionalInstrument;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** All settings, split into three tabs. */
public final class SettingsScreen extends Screen {
    private static final int PANEL_W = 460;

    private enum Tab {
        GENERAL,
        NOTE_MAP
    }

    private final Screen parent;
    private final NotebotConfig config = NotebotMod.config();
    private final UiKit.Hotspots hotspots = new UiKit.Hotspots();
    private final List<Row> rows = new ArrayList<>();

    private Tab tab = Tab.GENERAL;
    private int scroll;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Notebot Settings"));
        this.parent = parent;
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int right() {
        return left() + PANEL_W;
    }

    private int listTop() {
        return 96;
    }

    private int listBottom() {
        return height - 40;
    }

    @Override
    protected void init() {
        buildRows();
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ rows

    private void buildRows() {
        rows.clear();
        switch (tab) {
            case GENERAL -> {
                rows.add(intRow("Tick delay", () -> config.tickDelay, v -> config.tickDelay = v, 1, 20));
                rows.add(intRow("Concurrent tune blocks", () -> config.concurrentTuneBlocks, v -> config.concurrentTuneBlocks = v, 1, 20));
                rows.add(enumRow("Mode", NotebotMode.values(), () -> config.mode, v -> config.mode = v));
                rows.add(enumRow("Instrument detect", InstrumentDetectMode.values(),
                    () -> config.instrumentDetectMode, v -> config.instrumentDetectMode = v));
                rows.add(boolRow("Polyphonic", () -> config.polyphonic, v -> config.polyphonic = v));
                rows.add(boolRow("Auto rotate", () -> config.autoRotate, v -> config.autoRotate = v));
                rows.add(boolRow("Swing arm", () -> config.swingArm, v -> config.swingArm = v));
                rows.add(boolRow("Transpose out of range", () -> config.transposeOutOfRange, v -> config.transposeOutOfRange = v));
                rows.add(intRow("Re-check delay", () -> config.checkNoteblocksAgainDelay, v -> config.checkNoteblocksAgainDelay = v, 1, 20));
                rows.add(header("Portable output (/playsound)"));
                rows.add(enumRow("Target", PortableTarget.values(), () -> config.portableTarget, v -> config.portableTarget = v));
                rows.add(intRow("Volume %", () -> Math.round(config.portableVolume * 100),
                    v -> config.portableVolume = v / 100.0f, 0, 400));
                rows.add(intRow("Min volume %", () -> Math.round(config.portableMinVolume * 100),
                    v -> config.portableMinVolume = v / 100.0f, 0, 100));
                rows.add(intRow("Max commands / tick", () -> config.portableMaxCommandsPerTick,
                    v -> config.portableMaxCommandsPerTick = v, 1, 32));
            }
            case NOTE_MAP -> {
                rows.add(header("Maps a song instrument to the instrument actually played"));
                for (NoteBlockInstrument instrument : NoteBlockInstrument.values()) {
                    rows.add(enumRow(pretty(instrument.name()), OptionalInstrument.values(),
                        () -> config.optionalInstrument(instrument),
                        v -> config.setMappedInstrument(instrument, v)));
                }
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * Row.HEIGHT - (listBottom() - listTop()));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (hotspots.click(event.x(), event.y())) {
            buildRows();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= left() && mouseX < right() && mouseY >= listTop() && mouseY < listBottom()) {
            scroll = Math.max(0, Math.min(scroll - (int) (scrollY * Row.HEIGHT * 2), maxScroll()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hotspots.clear();

        int l = left();
        int r = right();

        UiKit.rect(g, l - 8, 24, r + 8, height - 24, UiKit.PANEL);
        UiKit.frame(g, l - 8, 24, r + 8, height - 24, UiKit.BORDER);
        UiKit.centered(g, font, "Notebot Settings", (l + r) / 2, 32, UiKit.TEXT);

        // Tabs.
        int tabY = 52;
        int tabW = (PANEL_W - 8) / 2;
        drawTab(g, l, tabY, tabW, Tab.GENERAL, "General", mouseX, mouseY);
        drawTab(g, l + tabW + 8, tabY, tabW, Tab.NOTE_MAP, "Note map", mouseX, mouseY);

        // Rows.
        int top = listTop();
        int bottom = listBottom();
        UiKit.rect(g, l, top, r, bottom, 0x40000000);
        g.enableScissor(l, top, r, bottom);

        int y = top - scroll;
        for (Row row : rows) {
            row.draw(g, font, hotspots, l, y, PANEL_W, mouseX, mouseY);
            y += Row.HEIGHT;
        }

        g.disableScissor();

        if (maxScroll() > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(12, trackH * trackH / (rows.size() * Row.HEIGHT));
            int thumbY = top + (trackH - thumbH) * scroll / maxScroll();
            UiKit.rect(g, r - 3, thumbY, r - 1, thumbY + thumbH, UiKit.ACCENT_DIM);
        }

        boolean doneHovered = mouseX >= r - 76 && mouseX < r - 16 && mouseY >= height - 38 && mouseY < height - 24;
        UiKit.button(g, font, hotspots, r - 76, height - 38, 60, 14, "Done", doneHovered, this::onClose);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawTab(GuiGraphicsExtractor g, int x, int y, int w, Tab value, String label, int mouseX, int mouseY) {
        boolean active = tab == value;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 20;
        g.fill(x, y, x + w, y + 20, active ? UiKit.ACCENT_DIM : (hovered ? UiKit.BUTTON_HOVER : UiKit.BUTTON));
        g.outline(x, y, w, 20, UiKit.BORDER);
        UiKit.centered(g, font, label, x + w / 2, y + 6, active ? UiKit.TEXT : UiKit.TEXT_DIM);
        hotspots.add(x, y, x + w, y + 20, () -> {
            tab = value;
            scroll = 0;
            buildRows();
        });
    }

    // ------------------------------------------------------------------ row types

    private abstract static class Row {
        static final int HEIGHT = 22;
        final String label;

        Row(String label) {
            this.label = label;
        }

        abstract void draw(GuiGraphicsExtractor g, Font font, UiKit.Hotspots hotspots,
                           int x, int y, int w, int mouseX, int mouseY);

        static boolean hovered(int mouseX, int mouseY, int x1, int y1, int x2, int y2) {
            return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
        }
    }

    private static final class HeaderRow extends Row {
        HeaderRow(String label) {
            super(label);
        }

        @Override
        void draw(GuiGraphicsExtractor g, Font font, UiKit.Hotspots hotspots, int x, int y, int w, int mouseX, int mouseY) {
            UiKit.text(g, font, label, x + 6, y + 8, UiKit.ACCENT);
        }
    }

    private static final class IntRow extends Row {
        private final IntSupplier get;
        private final IntConsumer set;
        private final int min;
        private final int max;

        IntRow(String label, IntSupplier get, IntConsumer set, int min, int max) {
            super(label);
            this.get = get;
            this.set = set;
            this.min = min;
            this.max = max;
        }

        @Override
        void draw(GuiGraphicsExtractor g, Font font, UiKit.Hotspots hotspots, int x, int y, int w, int mouseX, int mouseY) {
            UiKit.text(g, font, label, x + 6, y + 7, UiKit.TEXT);

            int plusX = x + w - 24;
            int minusX = plusX - 62;
            String value = String.valueOf(get.getAsInt());
            UiKit.text(g, font, value, minusX + 26 + (40 - font.width(value)) / 2, y + 7, UiKit.TEXT);

            boolean minusHovered = hovered(mouseX, mouseY, minusX, y + 2, minusX + 18, y + 20);
            UiKit.button(g, font, hotspots, minusX, y + 2, 18, 18, "-", minusHovered, () -> {
                int next = Math.max(min, get.getAsInt() - 1);
                set.accept(next);
                save();
            });

            boolean plusHovered = hovered(mouseX, mouseY, plusX, y + 2, plusX + 18, y + 20);
            UiKit.button(g, font, hotspots, plusX, y + 2, 18, 18, "+", plusHovered, () -> {
                int next = Math.min(max, get.getAsInt() + 1);
                set.accept(next);
                save();
            });
        }
    }

    private static final class BoolRow extends Row {
        private final Supplier<Boolean> get;
        private final java.util.function.Consumer<Boolean> set;

        BoolRow(String label, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
            super(label);
            this.get = get;
            this.set = set;
        }

        @Override
        void draw(GuiGraphicsExtractor g, Font font, UiKit.Hotspots hotspots, int x, int y, int w, int mouseX, int mouseY) {
            UiKit.text(g, font, label, x + 6, y + 7, UiKit.TEXT);
            boolean on = get.get();
            String value = on ? "ON" : "OFF";
            int bx = x + w - 60;
            boolean hovered = hovered(mouseX, mouseY, bx, y + 2, bx + 54, y + 20);
            UiKit.button(g, font, hotspots, bx, y + 2, 54, 18, value, hovered, () -> {
                set.accept(!on);
                save();
            });
        }
    }

    private static final class EnumRow<T extends Enum<T>> extends Row {
        private final T[] values;
        private final Supplier<T> get;
        private final java.util.function.Consumer<T> set;

        EnumRow(String label, T[] values, Supplier<T> get, java.util.function.Consumer<T> set) {
            super(label);
            this.values = values;
            this.get = get;
            this.set = set;
        }

        @Override
        void draw(GuiGraphicsExtractor g, Font font, UiKit.Hotspots hotspots, int x, int y, int w, int mouseX, int mouseY) {
            UiKit.text(g, font, label, x + 6, y + 7, UiKit.TEXT);

            T value = get.get();
            String text = value == null ? "?" : UiKit.trim(font, pretty(value.name()), 120);

            int leftArrow = x + w - 118;
            int rightArrow = x + w - 24;
            UiKit.text(g, font, text, leftArrow + 20 + (74 - font.width(text)) / 2, y + 7, UiKit.TEXT);

            boolean leftHovered = hovered(mouseX, mouseY, leftArrow, y + 2, leftArrow + 18, y + 20);
            UiKit.button(g, font, hotspots, leftArrow, y + 2, 18, 18, "<", leftHovered, () -> {
                set.accept(step(value, -1));
                save();
            });

            boolean rightHovered = hovered(mouseX, mouseY, rightArrow, y + 2, rightArrow + 18, y + 20);
            UiKit.button(g, font, hotspots, rightArrow, y + 2, 18, 18, ">", rightHovered, () -> {
                set.accept(step(value, 1));
                save();
            });
        }

        private T step(T value, int direction) {
            int index = Math.floorMod(value.ordinal() + direction, values.length);
            return values[index];
        }
    }

    private static HeaderRow header(String label) {
        return new HeaderRow(label);
    }

    private static IntRow intRow(String label, IntSupplier get, IntConsumer set, int min, int max) {
        return new IntRow(label, get, set, min, max);
    }

    private static BoolRow boolRow(String label, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
        return new BoolRow(label, get, set);
    }

    private static <T extends Enum<T>> EnumRow<T> enumRow(String label, T[] values, Supplier<T> get,
                                                         java.util.function.Consumer<T> set) {
        return new EnumRow<>(label, values, get, set);
    }

    private static void save() {
        NotebotMod.config().save();
    }

    private static String pretty(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
