/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

import dev.notebot.NotebotMod;
import dev.notebot.config.NotebotConfig;
import dev.notebot.playback.OutputBackend;
import dev.notebot.playback.PlayMode;
import dev.notebot.playback.PlaybackEngine;
import dev.notebot.song.Song;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Music-player style "now playing" screen with a draggable progress bar. */
public final class PlayerScreen extends Screen {
    private static final int PANEL_W = 400;

    private final Screen parent;
    private final PlaybackEngine engine = NotebotMod.engine();
    private final NotebotConfig config = NotebotMod.config();
    private final UiKit.Hotspots hotspots = new UiKit.Hotspots();

    private boolean dragging;

    public PlayerScreen(Screen parent) {
        super(Component.literal("Notebot Player"));
        this.parent = parent;
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int right() {
        return left() + PANEL_W;
    }

    private int sliderY() {
        return 128;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inSlider(event.x(), event.y())) {
            if (engine.getState() == PlaybackEngine.State.PLAYING) {
                dragging = true;
                seek(event.x());
                return true;
            }
        }
        if (hotspots.click(event.x(), event.y())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            seek(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean inSlider(double mx, double my) {
        return mx >= left() + 16 && mx <= right() - 16 && my >= sliderY() - 6 && my <= sliderY() + 14;
    }

    private void seek(double mouseX) {
        float progress = (float) ((mouseX - (left() + 16)) / (PANEL_W - 32));
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        engine.seekToMillis((long) (progress * engine.getLastTick() * 50L));
    }

    // ------------------------------------------------------------------ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hotspots.clear();

        int l = left();
        int r = right();
        int top = 40;
        int bottom = 244;

        UiKit.rect(g, l, top, r, bottom, UiKit.PANEL);
        UiKit.frame(g, l, top, r, bottom, UiKit.BORDER);

        Song song = engine.getSong();
        String title = song == null ? "No song loaded" : engine.getDisplayTitle();
        String author = song == null ? "" : engine.getDisplayAuthor();

        UiKit.centered(g, font, UiKit.trim(font, title, PANEL_W - 32), (l + r) / 2, top + 18, UiKit.TEXT);
        if (!author.isBlank()) {
            UiKit.centered(g, font, UiKit.trim(font, author, PANEL_W - 32), (l + r) / 2, top + 34, UiKit.TEXT_DIM);
        }

        // Backend + loop mode.
        String backendLabel = Lang.text("Output") + ": " + pretty(config.backend.name());
        boolean backendHovered = in(mouseX, mouseY, l + 16, 72, l + 16 + font.width(backendLabel) + 12, 90);
        UiKit.button(g, font, hotspots, l + 16, 72, font.width(backendLabel) + 12, 18, backendLabel, backendHovered,
            this::cycleBackend);

        String loopLabel = Lang.text("Mode") + ": " + pretty(config.playMode.name());
        int loopW = font.width(loopLabel) + 12;
        boolean loopHovered = in(mouseX, mouseY, r - 16 - loopW, 72, r - 16, 90);
        UiKit.button(g, font, hotspots, r - 16 - loopW, 72, loopW, 18, loopLabel, loopHovered, this::cyclePlayMode);

        // Progress.
        float progress = engine.getLastTick() <= 0 ? 0.0f : engine.getTick() / (float) engine.getLastTick();
        UiKit.bar(g, l + 16, sliderY(), PANEL_W - 32, 8, progress, UiKit.ACCENT);

        int knobX = l + 16 + (int) ((PANEL_W - 32) * Math.max(0.0f, Math.min(1.0f, progress)));
        g.fill(knobX - 3, sliderY() - 3, knobX + 4, sliderY() + 11, 0xFFFFFFFF);
        g.outline(knobX - 3, sliderY() - 3, 7, 14, UiKit.BORDER);

        long position = engine.getTick() * 50L;
        long length = engine.getLastTick() * 50L;
        UiKit.text(g, font, UiKit.formatTime(position), l + 16, sliderY() + 12, UiKit.TEXT_DIM);
        String total = UiKit.formatTime(length);
        UiKit.text(g, font, total, r - 16 - font.width(total), sliderY() + 12, UiKit.TEXT_DIM);

        String status = translateStatus(engine.getStatus());
        if (engine.getState() == PlaybackEngine.State.PREPARING) {
            UiKit.centered(g, font, UiKit.trim(font, status, PANEL_W - 32), (l + r) / 2, sliderY() + 24, 0xFFFFC94A);
        }

        // Transport.
        int buttonY = 172;
        int center = (l + r) / 2;
        int x = center - 110;

        transport(g, hotspots, x, buttonY, 52, "Prev", mouseX, mouseY, engine::previous);
        x += 56;
        transport(g, hotspots, x, buttonY, 60,
            engine.getState() == PlaybackEngine.State.PLAYING ? "Pause" : "Play", mouseX, mouseY, engine::togglePause);
        x += 64;
        transport(g, hotspots, x, buttonY, 52, "Next", mouseX, mouseY, engine::next);
        x += 56;
        transport(g, hotspots, x, buttonY, 48, "Stop", mouseX, mouseY, engine::stop);

        // Volume: follows whichever backend is actually producing sound.
        int volumeY = 206;
        OutputBackend volumeBackend = volumeBackend();

        if (volumeBackend == OutputBackend.NOTEBLOCK) {
            UiKit.text(g, font, "Volume: fixed by the game (note blocks)", l + 16, volumeY + 5, UiKit.TEXT_DIM);
        } else {
            String label = volumeBackend == OutputBackend.PORTABLE ? "Volume (portable)" : "Volume (preview)";
            UiKit.text(g, font, label, l + 16, volumeY + 4, UiKit.TEXT_DIM);

            int volumePercent = Math.round(currentVolume() * 100);
            boolean minusHovered = in(mouseX, mouseY, l + 112, volumeY, l + 132, volumeY + 18);
            UiKit.button(g, font, hotspots, l + 112, volumeY, 20, 18, "-", minusHovered, () -> changeVolume(-10));
            String volumeText = volumePercent + "%";
            UiKit.text(g, font, volumeText, l + 136, volumeY + 5, UiKit.TEXT);
            boolean plusHovered = in(mouseX, mouseY, l + 176, volumeY, l + 196, volumeY + 18);
            UiKit.button(g, font, hotspots, l + 176, volumeY, 20, 18, "+", plusHovered, () -> changeVolume(10));
        }

        boolean backHovered = in(mouseX, mouseY, r - 76, volumeY, r - 16, volumeY + 18);
        UiKit.button(g, font, hotspots, r - 76, volumeY, 60, 18, "Back", backHovered, this::onClose);

        String current = engine.getSongPath();
        if (current != null) {
            boolean favorite = config.isFavorite(current);
            int starX = r - 102;
            boolean starHovered = in(mouseX, mouseY, starX, volumeY, starX + 20, volumeY + 18);
            UiKit.heartButton(g, hotspots, starX, volumeY, 20, 18, favorite, starHovered, () -> {
                config.setFavorite(current, !config.isFavorite(current));
                config.save();
            });
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void transport(GuiGraphicsExtractor g, UiKit.Hotspots hotspots, int x, int y, int w,
                           String label, int mouseX, int mouseY, Runnable action) {
        boolean hovered = in(mouseX, mouseY, x, y, x + w, y + 20);
        UiKit.button(g, font, hotspots, x, y, w, 20, label, hovered, action);
    }

    private void cycleBackend() {
        OutputBackend[] values = OutputBackend.values();
        config.backend = values[(config.backend.ordinal() + 1) % values.length];
        config.save();
    }

    private void cyclePlayMode() {
        PlayMode[] values = PlayMode.values();
        config.playMode = values[(config.playMode.ordinal() + 1) % values.length];
        config.save();
    }

    private void changeVolume(int delta) {
        float value = Math.max(0.0f, Math.min(2.0f, currentVolume() + delta / 100.0f));
        if (volumeBackend() == OutputBackend.PORTABLE) {
            config.portableVolume = value;
        } else {
            config.volume = value;
        }
        config.save();
    }

    /** Backend that is currently audible; falls back to the configured one when nothing plays. */
    private OutputBackend volumeBackend() {
        OutputBackend active = engine.getActiveBackend();
        return active != null ? active : config.backend;
    }

    private float currentVolume() {
        return volumeBackend() == OutputBackend.PORTABLE ? config.portableVolume : config.volume;
    }

    private static String pretty(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        String name = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        return Lang.text(name);
    }

    /** "Preparing: Tuning" -> both halves translated. */
    private static String translateStatus(String status) {
        int separator = status.indexOf(": ");
        if (separator > 0) {
            return Lang.text(status.substring(0, separator)) + ": " + Lang.text(status.substring(separator + 2));
        }
        return Lang.text(status);
    }

    private static boolean in(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }
}
