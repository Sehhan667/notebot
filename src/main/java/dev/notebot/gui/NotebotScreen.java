/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

import dev.notebot.NotebotMod;
import dev.notebot.config.NotebotConfig;
import dev.notebot.library.SongEntry;
import dev.notebot.library.SongLibrary;
import dev.notebot.playback.PlaybackEngine;
import dev.notebot.song.Song;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Library browser: nested folders, favourites, search, and a compact now-playing bar. */
public final class NotebotScreen extends Screen {
    private static final int PANEL_W = 440;

    private final SongLibrary library = NotebotMod.library();
    private final PlaybackEngine engine = NotebotMod.engine();
    private final NotebotConfig config = NotebotMod.config();
    private final UiKit.Hotspots hotspots = new UiKit.Hotspots();

    private final List<Row> rows = new ArrayList<>();
    private EditBox search;
    private String filter = "";
    private boolean favoritesOnly;
    private String folder = "";
    private int scroll;
    private boolean draggingProgress;
    private boolean draggingScroll;
    private boolean scanning;

    private record Row(String label, String path, SongEntry entry, boolean isFolder) {
    }

    public NotebotScreen() {
        super(Component.literal("Notebot"));
    }

    // ------------------------------------------------------------------ layout

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int right() {
        return left() + PANEL_W;
    }

    private int listTop() {
        return 92;
    }

    private int listBottom() {
        return height - 74;
    }

    private int progressY() {
        return height - 56;
    }

    private int barX() {
        return left() + 8;
    }

    private int barWidth() {
        return PANEL_W - 16;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        // Restore what the previous screen instance had.
        folder = UiState.folder;
        filter = UiState.filter;
        favoritesOnly = UiState.favoritesOnly;
        scroll = UiState.scroll;

        search = new EditBox(font, left(), 60, PANEL_W - 176, 18, Component.literal("Search"));
        search.setMaxLength(64);
        search.setHint(Lang.ui("Search songs…"));
        search.setValue(filter);
        search.setResponder(value -> {
            filter = value;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(search);

        refresh();
        reloadLibrary();
    }

    @Override
    public void removed() {
        UiState.folder = folder;
        UiState.filter = filter;
        UiState.favoritesOnly = favoritesOnly;
        UiState.scroll = scroll;
    }

    private void refresh() {
        rows.clear();

        if (!filter.isBlank()) {
            List<SongEntry> found = favoritesOnly ? favoritesMatching() : library.search(filter);
            for (SongEntry entry : found) rows.add(new Row(entry.displayTitle(), entry.relativePath(), entry, false));
        } else if (favoritesOnly) {
            for (SongEntry entry : library.favorites()) {
                rows.add(new Row(entry.displayTitle(), entry.relativePath(), entry, false));
            }
        } else {
            for (String sub : library.subFolders(folder)) {
                String path = folder.isEmpty() ? sub : folder + "/" + sub;
                rows.add(new Row(sub, path, null, true));
            }
            for (SongEntry entry : library.songsIn(folder, false)) {
                rows.add(new Row(entry.displayTitle(), entry.relativePath(), entry, false));
            }
        }

        scroll = clampScroll(UiState.scroll);
    }

    private List<SongEntry> favoritesMatching() {
        String needle = filter.toLowerCase(java.util.Locale.ROOT);
        List<SongEntry> result = new ArrayList<>();
        for (SongEntry entry : library.favorites()) {
            boolean matches = entry.relativePath().toLowerCase(java.util.Locale.ROOT).contains(needle)
                || (entry.title() != null && entry.title().toLowerCase(java.util.Locale.ROOT).contains(needle))
                || (entry.author() != null && entry.author().toLowerCase(java.util.Locale.ROOT).contains(needle));
            if (matches) result.add(entry);
        }
        return result;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * UiKit.ROW_H - (listBottom() - listTop()));
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(value, maxScroll()));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        if (mx >= left() && mx < right() && my >= progressY() - 6 && my <= progressY() + 10) {
            if (engine.getState() == PlaybackEngine.State.PLAYING) {
                draggingProgress = true;
                seekFromMouse(mx);
                return true;
            }
        }

        if (isOnScrollbar(mx, my)) {
            draggingScroll = true;
            scrollFromMouse(my);
            return true;
        }

        if (hotspots.click(mx, my)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOnScrollbar(double mx, double my) {
        return maxScroll() > 0
            && mx >= right() - 10 && mx <= right()
            && my >= listTop() && my < listBottom();
    }

    private void scrollFromMouse(double mouseY) {
        int trackH = listBottom() - listTop();
        int thumbH = Math.max(12, trackH * trackH / Math.max(1, rows.size() * UiKit.ROW_H));
        double ratio = (mouseY - listTop() - thumbH / 2.0) / Math.max(1, trackH - thumbH);
        scroll = clampScroll((int) (ratio * maxScroll()));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingProgress) {
            seekFromMouse(event.x());
            return true;
        }
        if (draggingScroll) {
            scrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingProgress) {
            draggingProgress = false;
            return true;
        }
        if (draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void seekFromMouse(double mouseX) {
        float progress = (float) ((mouseX - barX()) / barWidth());
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        engine.seekToMillis((long) (progress * engine.getLastTick() * 50L));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= left() && mouseX < right() && mouseY >= listTop() && mouseY < listBottom()) {
            scroll = clampScroll(scroll - (int) (scrollY * UiKit.ROW_H * 2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hotspots.clear();

        int l = left();
        int r = right();

        UiKit.rect(g, l - 8, 24, r + 8, height - 24, UiKit.PANEL);
        UiKit.frame(g, l - 8, 24, r + 8, height - 24, UiKit.BORDER);
        UiKit.centered(g, font, "Notebot", (l + r) / 2, 32, UiKit.TEXT);

        String crumb = "notebot/" + (folder.isEmpty() ? "" : folder + "/");
        UiKit.text(g, font, crumb, l, 44, UiKit.TEXT_DIM);

        int topY = 60;
        boolean favHovered = in(mouseX, mouseY, r - 168, topY, r - 92, topY + 18);
        UiKit.button(g, font, hotspots, r - 168, topY, 76, 18,
            favoritesOnly ? "Favourites: on" : "Favourites", favHovered, () -> {
                favoritesOnly = !favoritesOnly;
                scroll = 0;
                refresh();
            });

        boolean reloadHovered = in(mouseX, mouseY, r - 88, topY, r, topY + 18);
        UiKit.button(g, font, hotspots, r - 88, topY, 88, 18, "Reload", reloadHovered, this::reloadLibrary);

        if (!folder.isEmpty()) {
            boolean upHovered = in(mouseX, mouseY, l, topY, l + 52, topY + 18);
            UiKit.button(g, font, hotspots, l, topY, 52, 18, "Up", upHovered, () -> {
                int slash = folder.lastIndexOf('/');
                folder = slash < 0 ? "" : folder.substring(0, slash);
                scroll = 0;
                refresh();
            });
        }

        drawRows(g, mouseX, mouseY, l, r);
        drawNowPlaying(g, mouseX, mouseY, l, r);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawRows(GuiGraphicsExtractor g, int mouseX, int mouseY, int l, int r) {
        int top = listTop();
        int bottom = listBottom();

        UiKit.rect(g, l, top, r, bottom, 0x40000000);
        g.enableScissor(l, top, r, bottom);

        if (rows.isEmpty()) {
            UiKit.centered(g, font, scanning ? "Scanning…" : "No songs found", (l + r) / 2, top + 8, UiKit.TEXT_DIM);
            String root = library.getRoot().toString();
            UiKit.centered(g, font, UiKit.trim(font, root, PANEL_W - 24), (l + r) / 2, top + 22, UiKit.TEXT_DIM);
        }

        String playingPath = engine.getSongPath();
        int y = top - scroll;
        for (Row row : rows) {
            if (y + UiKit.ROW_H > top && y < bottom) {
                boolean hovered = in(mouseX, mouseY, l, y, r, y + UiKit.ROW_H);
                boolean playing = !row.isFolder() && playingPath != null && playingPath.equals(row.path());

                if (playing) {
                    UiKit.rect(g, l, y, r, y + UiKit.ROW_H, 0x558B6FE8);
                } else if (hovered) {
                    UiKit.rect(g, l, y, r, y + UiKit.ROW_H, UiKit.ROW_HOVER);
                }

                String prefix = row.isFolder() ? "▸ " : (playing ? "▶ " : "♪ ");
                int textX = l + 6;
                int starX = r - 20;

                int labelWidth = (starX - 8) - textX;
                String label = UiKit.trim(font, prefix + row.label(), labelWidth);
                int color = playing ? 0xFFFFFFFF : (row.isFolder() ? UiKit.ACCENT : UiKit.TEXT);
                UiKit.text(g, font, label, textX, y + 6, color);

                // Show the file name in grey when the row is labelled with the song's own title.
                if (row.entry() != null) {
                    String base = row.entry().baseName();
                    if (!base.equals(row.label())) {
                        String suffix = "  " + base;
                        if (font.width(label) + font.width(suffix) < labelWidth) {
                            UiKit.text(g, font, suffix, textX + font.width(label), y + 6, UiKit.TEXT_DIM);
                        }
                    }
                }

                if (!row.isFolder()) {
                    boolean favorite = config.isFavorite(row.path());
                    UiKit.heart(g, starX - 2, y + 5, 2, favorite ? 0xFFFF5C7A : 0xFF6A6A76);
                    hotspots.add(starX - 4, y, r - 2, y + UiKit.ROW_H, () -> {
                        config.setFavorite(row.path(), !config.isFavorite(row.path()));
                        config.save();
                    });
                }

                hotspots.add(l, y, starX - 6, y + UiKit.ROW_H, () -> onRowActivated(row));
            }
            y += UiKit.ROW_H;
        }

        g.disableScissor();

        if (maxScroll() > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(12, trackH * trackH / (rows.size() * UiKit.ROW_H));
            int thumbY = top + (trackH - thumbH) * scroll / maxScroll();

            UiKit.rect(g, r - 5, top, r - 2, bottom, 0x40000000);
            UiKit.rect(g, r - 5, thumbY, r - 2, thumbY + thumbH, UiKit.ACCENT);
        }
    }

    private void onRowActivated(Row row) {
        if (row.isFolder()) {
            folder = row.path();
            scroll = 0;
            refresh();
            return;
        }

        SongEntry entry = row.entry();
        if (entry == null) return;

        List<SongEntry> queue = new ArrayList<>();
        for (Row candidate : rows) {
            if (!candidate.isFolder() && candidate.entry() != null) queue.add(candidate.entry());
        }
        int index = queue.indexOf(entry);
        if (index < 0) {
            queue = List.of(entry);
            index = 0;
        }

        engine.playAt(queue, index);
    }

    private void reloadLibrary() {
        if (scanning) return;
        scanning = true;

        CompletableFuture.runAsync(library::rescan).thenRun(() ->
            minecraft.execute(() -> {
                scanning = false;
                if (minecraft.gui.screen() == this) refresh();
            }));
    }

    private void drawNowPlaying(GuiGraphicsExtractor g, int mouseX, int mouseY, int l, int r) {
        int barTop = height - 74;
        UiKit.rect(g, l, barTop, r, height - 24, UiKit.PANEL_LIGHT);

        Song song = engine.getSong();
        String title = song == null ? "No song loaded" : engine.getDisplayTitle();
        String author = song == null ? "" : engine.getDisplayAuthor();
        String info = UiKit.trim(font, title + (author.isBlank() ? "" : " — " + author), r - l - 150);

        UiKit.text(g, font, info, l + 8, barTop + 6, UiKit.TEXT);

        long position = engine.getTick() * 50L;
        long length = engine.getLastTick() * 50L;
        String time = UiKit.formatTime(position) + " / " + UiKit.formatTime(length);
        int timeX = r - 8 - font.width(time);
        UiKit.text(g, font, time, timeX, barTop + 6, UiKit.TEXT_DIM);

        // Favourite the track that is actually playing.
        String current = engine.getSongPath();
        if (current != null) {
            boolean favorite = config.isFavorite(current);
            int starX = timeX - 22;
            boolean starHovered = in(mouseX, mouseY, starX, barTop + 2, starX + 16, barTop + 18);
            UiKit.heartButton(g, hotspots, starX, barTop + 2, 16, 16, favorite, starHovered, () -> {
                config.setFavorite(current, !config.isFavorite(current));
                config.save();
            });
        }

        float progress = engine.getLastTick() <= 0 ? 0.0f : engine.getTick() / (float) engine.getLastTick();
        UiKit.bar(g, barX(), progressY(), barWidth(), 4, progress, UiKit.ACCENT);

        // Draggable knob, so it is obvious the bar can be grabbed.
        int knobX = barX() + (int) (barWidth() * Math.max(0.0f, Math.min(1.0f, progress)));
        g.fill(knobX - 3, progressY() - 3, knobX + 4, progressY() + 7, 0xFFFFFFFF);
        g.outline(knobX - 3, progressY() - 3, 7, 10, UiKit.BORDER);

        int buttonY = progressY() + 8;
        int x = l + 8;

        transport(g, hotspots, x, buttonY, 44, "Prev", mouseX, mouseY, engine::previous);
        x += 48;
        transport(g, hotspots, x, buttonY, 52,
            engine.getState() == PlaybackEngine.State.PLAYING ? "Pause" : "Play", mouseX, mouseY, engine::togglePause);
        x += 56;
        transport(g, hotspots, x, buttonY, 44, "Next", mouseX, mouseY, engine::next);
        x += 48;
        transport(g, hotspots, x, buttonY, 44, "Stop", mouseX, mouseY, engine::stop);

        int rightX = r - 8 - 64;
        transport(g, hotspots, rightX - 60, buttonY, 56, "Player", mouseX, mouseY,
            () -> minecraft.gui.setScreen(new PlayerScreen(this)));
        transport(g, hotspots, rightX, buttonY, 64, "Settings", mouseX, mouseY,
            () -> minecraft.gui.setScreen(new SettingsScreen(this)));
    }

    private void transport(GuiGraphicsExtractor g, UiKit.Hotspots hotspots, int x, int y, int w,
                           String label, int mouseX, int mouseY, Runnable action) {
        boolean hovered = in(mouseX, mouseY, x, y, x + w, y + 16);
        UiKit.button(g, font, hotspots, x, y, w, 16, label, hovered, action);
    }

    private static boolean in(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }
}
