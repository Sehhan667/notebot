/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import dev.notebot.NotebotMod;
import dev.notebot.config.NotebotConfig;
import dev.notebot.library.SongEntry;
import dev.notebot.library.SongLibrary;
import dev.notebot.song.Song;
import dev.notebot.song.SongDecoders;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Queue + timeline. It owns "which tick of which song", nothing else; the actual sound is
 * delegated to a {@link NoteOutput} so preview / noteblock / portable behave identically.
 */
public final class PlaybackEngine {
    public enum State {
        IDLE,
        PREPARING,
        PLAYING,
        PAUSED
    }

    private final NotebotConfig config;
    private final SongLibrary library;

    private final List<SongEntry> queue = new ArrayList<>();
    private int queueIndex = -1;

    private Song song;
    private String songPath;
    private State state = State.IDLE;
    private int tick;

    private NoteOutput output;
    private OutputBackend activeBackend;
    private int consecutiveFailures;
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastDimension;
    private final Random random = new Random();

    /** Snapshot kept when the player leaves the world, so a quick rejoin resumes the song. */
    private record Saved(String songPath, int tick, java.util.List<String> queue, int index) {
    }

    private Saved saved;
    private long savedAt;
    private int pendingSeek = -1;

    public PlaybackEngine(NotebotConfig config, SongLibrary library) {
        this.config = config;
        this.library = library;
    }

    // ------------------------------------------------------------------ queue

    public void setQueue(List<SongEntry> entries, int index) {
        queue.clear();
        queue.addAll(entries);
        queueIndex = Math.max(-1, Math.min(index, queue.size() - 1));
    }

    public List<SongEntry> getQueue() {
        return List.copyOf(queue);
    }

    public int getQueueIndex() {
        return queueIndex;
    }

    public SongEntry getCurrentEntry() {
        return queueIndex >= 0 && queueIndex < queue.size() ? queue.get(queueIndex) : null;
    }

    // ------------------------------------------------------------------ transport

    public void playAt(List<SongEntry> entries, int index) {
        consecutiveFailures = 0;
        setQueue(entries, index);
        playCurrent();
    }

    public void playCurrent() {
        SongEntry entry = getCurrentEntry();
        if (entry == null) {
            stop();
            return;
        }

        stopOutput();

        File file = entry.resolve(library.getRoot()).toFile();
        if (!file.isFile()) {
            NotebotMod.LOG.warn("Song file disappeared: {}", file);
            giveUpOrAdvance("File not found: " + entry.relativePath());
            return;
        }

        try {
            Song parsed = SongDecoders.parse(file, config, NotebotMod::warnToChat, config.backend);
            consecutiveFailures = 0;
            begin(entry, parsed);
        } catch (Exception e) {
            NotebotMod.LOG.error("Failed to parse song {}", file, e);
            giveUpOrAdvance("Failed to load " + entry.relativePath());
        }
    }

    /** Skips to the next queue entry, but gives up once the whole queue failed to load. */
    private void giveUpOrAdvance(String message) {
        NotebotMod.warnToChat(message);
        consecutiveFailures++;
        if (consecutiveFailures > Math.max(1, queue.size())) {
            consecutiveFailures = 0;
            NotebotMod.warnToChat("No playable song in this queue.");
            stop();
            return;
        }
        advance(1);
    }

    private void begin(SongEntry entry, Song parsed) {
        this.song = parsed;
        this.songPath = entry.relativePath();
        this.tick = 0;
        this.lastDimension = net.minecraft.client.Minecraft.getInstance().level == null
            ? null
            : net.minecraft.client.Minecraft.getInstance().level.dimension();

        // Silence vanilla background music and announce the song like a music disc does.
        VanillaMusic.suppressNow();
        dev.notebot.gui.NotebotToast.nowPlaying(getDisplayTitle(), getDisplayAuthor());

        this.activeBackend = config.backend;
        this.output = createOutput(activeBackend);
        this.output.onStart(entry.relativePath(), parsed);

        this.state = output.ready() ? State.PLAYING : State.PREPARING;
    }

    private NoteOutput createOutput(OutputBackend backend) {
        return switch (backend) {
            case PREVIEW -> new PreviewOutput(config);
            case NOTEBLOCK -> new NoteblockOutput(config);
            case PORTABLE -> new PortableOutput(config);
        };
    }

    public void tick() {
        var mc = net.minecraft.client.Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            // Left the world (quit / disconnect / kicked): remember the position instead of dropping it.
            if (song != null) suspend();
            return;
        }

        if (saved != null) resumeIfPossible();

        if (state == State.IDLE || song == null) return;

        // Changing dimension invalidates noteblock positions; just stop rather than play ghosts.
        var dimension = mc.level.dimension();
        if (lastDimension != null && !lastDimension.equals(dimension)) {
            lastDimension = dimension;
            stop();
            return;
        }
        lastDimension = dimension;

        // Singleplayer pause (Esc) freezes the integrated server; keep the timeline frozen too,
        // otherwise the client keeps emitting notes while the world is standing still.
        if (mc.isPaused()) return;

        if (output != null && output.failed()) {
            NotebotMod.warnToChat(output.status());
            stop();
            return;
        }

        if (state == State.PREPARING) {
            if (output != null) output.onTick();
            if (output != null && output.ready()) {
                state = State.PLAYING;
                tick = pendingSeek >= 0 ? Math.min(pendingSeek, song.getLastTick()) : 0;
                pendingSeek = -1;
            }
            VanillaMusic.tickWhilePlaying();
            return;
        }

        if (state != State.PLAYING) return;

        VanillaMusic.tickWhilePlaying();

        if (output != null) output.onTick();

        if (tick > song.getLastTick()) {
            onSongEnd();
            return;
        }

        var notes = song.getNotesMap().get(tick);
        if (!notes.isEmpty() && output != null) {
            output.playNotes(notes);
        }

        tick++;
    }

    private void onSongEnd() {
        switch (config.playMode) {
            case LOOP_ONE -> {
                tick = 0;
                state = State.PLAYING;
            }
            case SEQUENTIAL -> {
                if (queueIndex + 1 < queue.size()) {
                    advance(1);
                } else {
                    stop();
                }
            }
            case LOOP_ALL -> advance(1);
            case SHUFFLE -> jumpRandom();
        }
    }

    /** Moves to another queue entry, wrapping when the current mode loops the queue. */
    private void advance(int direction) {
        if (queue.isEmpty()) {
            stop();
            return;
        }

        int next = queueIndex + direction;
        if (next < 0 || next >= queue.size()) {
            // Manual navigation follows the mode: loop modes wrap, sequential stops at the end.
            if (config.playMode == PlayMode.LOOP_ALL || config.playMode == PlayMode.LOOP_ONE) {
                next = Math.floorMod(next, queue.size());
            } else {
                if (direction > 0) {
                    stop();
                    return;
                }
                next = 0;
            }
        }

        queueIndex = next;
        playCurrent();
    }

    /** Jumps to a random other entry; used by SHUFFLE for both automatic and manual switching. */
    private void jumpRandom() {
        if (queue.isEmpty()) return;
        if (queue.size() == 1) {
            queueIndex = 0;
            playCurrent();
            return;
        }

        int next;
        do {
            next = random.nextInt(queue.size());
        } while (next == queueIndex);

        queueIndex = next;
        playCurrent();
    }

    public void next() {
        if (queue.isEmpty()) return;
        if (config.playMode == PlayMode.SHUFFLE) {
            jumpRandom();
            return;
        }
        advance(1);
    }

    public void previous() {
        if (queue.isEmpty()) return;
        // Music-player convention: restart the song first if we are more than 3s in.
        if (song != null && tick > 60) {
            tick = 0;
            return;
        }
        if (config.playMode == PlayMode.SHUFFLE) {
            jumpRandom();
            return;
        }
        advance(-1);
    }

    public void pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.PLAYING;
        }
    }

    public void togglePause() {
        if (state == State.PLAYING) pause();
        else if (state == State.PAUSED) resume();
    }

    public void stop() {
        stopOutput();
        song = null;
        songPath = null;
        state = State.IDLE;
        tick = 0;
        saved = null;
        pendingSeek = -1;
    }

    /**
     * Stops output but remembers the song and position, so rejoining the world shortly after
     * continues where it left off. Used when the world is left, not when the user presses Stop.
     */
    public void suspend() {
        if (song == null || songPath == null) {
            stop();
            return;
        }

        java.util.List<String> paths = new java.util.ArrayList<>();
        for (SongEntry entry : queue) paths.add(entry.relativePath());

        saved = new Saved(songPath, tick, paths, queueIndex);
        savedAt = System.currentTimeMillis();

        stopOutput();
        song = null;
        songPath = null;
        state = State.IDLE;
        tick = 0;
    }

    /** Rebuilds the saved queue and resumes from the saved tick, if the window has not expired. */
    private void resumeIfPossible() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (config.resumeWindowMinutes <= 0
            || System.currentTimeMillis() - savedAt > config.resumeWindowMinutes * 60_000L) {
            saved = null;
            return;
        }

        Saved snapshot = saved;
        saved = null;

        java.util.List<SongEntry> entries = new java.util.ArrayList<>();
        for (String path : snapshot.queue()) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            entries.add(new SongEntry(path, name, 0, 0, null, null));
        }
        if (entries.isEmpty()) return;

        setQueue(entries, Math.max(0, Math.min(snapshot.index(), entries.size() - 1)));
        pendingSeek = snapshot.tick();
        playCurrent();

        if (state == State.PLAYING && song != null && pendingSeek >= 0) {
            tick = Math.min(pendingSeek, song.getLastTick());
            pendingSeek = -1;
        }
    }

    private void stopOutput() {
        if (output != null) {
            output.onStop();
            output = null;
        }
        state = State.IDLE;
    }

    /**
     * Jumps to an absolute tick. Only allowed while actually playing, never during preparation:
     * tuning uses relative hit counters that must stay aligned with the block states.
     */
    public boolean seekToTick(int target) {
        if (state != State.PLAYING || song == null) return false;
        tick = Math.max(0, Math.min(target, song.getLastTick()));
        return true;
    }

    public boolean seekToMillis(long millis) {
        if (song == null) return false;
        return seekToTick((int) (millis / 50L));
    }

    // ------------------------------------------------------------------ queries

    public boolean isPlaying() {
        return state == State.PLAYING || state == State.PREPARING;
    }

    public State getState() {
        return state;
    }

    public Song getSong() {
        return song;
    }

    /** Song title, falling back to the file name when the file has no title metadata. */
    public String getDisplayTitle() {
        if (song == null) return null;
        String title = song.getTitle();
        if (title != null && !title.isBlank()) return title;
        if (songPath == null) return "";
        String name = songPath.substring(songPath.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    public String getDisplayAuthor() {
        return song == null || song.getAuthor() == null ? "" : song.getAuthor();
    }

    public String getSongPath() {
        return songPath;
    }

    public int getTick() {
        return tick;
    }

    public int getLastTick() {
        return song == null ? 0 : song.getLastTick();
    }

    public float getProgress() {
        if (song == null || song.getLastTick() <= 0) return 0.0f;
        return Math.min(1.0f, tick / (float) song.getLastTick());
    }

    public String getStatus() {
        if (state == State.IDLE || song == null) return "No song loaded";
        if (state == State.PREPARING) {
            String s = output == null ? "" : output.status();
            return s.isEmpty() ? "Preparing" : "Preparing: " + s;
        }
        if (state == State.PAUSED) return "Paused";
        return output != null && !output.status().isEmpty() ? output.status() : "Playing";
    }

    public OutputBackend getActiveBackend() {
        return activeBackend;
    }
}
