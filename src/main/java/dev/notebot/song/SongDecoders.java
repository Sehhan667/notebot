/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.song;

import dev.notebot.config.NotebotConfig;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Decoder registry plus the post-processing that adapts a parsed song to the current settings. */
public final class SongDecoders {
    private static final Map<String, SongDecoder> DECODERS = new HashMap<>();

    static {
        register("nbs", new NBSSongDecoder());
        register("txt", new TextSongDecoder());
    }

    private SongDecoders() {
    }

    public static void register(String extension, SongDecoder decoder) {
        DECODERS.put(extension.toLowerCase(Locale.ROOT), decoder);
    }

    public static boolean hasDecoder(File file) {
        return DECODERS.containsKey(extension(file.getName()));
    }

    public static boolean hasDecoder(Path path) {
        return hasDecoder(path.toFile());
    }

    public static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a song and applies the current settings (out-of-range transposition and instrument mapping).
     */
    public static Song parse(File file, NotebotConfig config, Consumer<String> warn,
                             dev.notebot.playback.OutputBackend backend) throws Exception {
        SongDecoder decoder = DECODERS.get(extension(file.getName()));
        if (decoder == null) throw new IllegalStateException("No decoder for " + file.getName());

        Song song = decoder.parse(file, warn);
        fixSong(song, config, warn, backend);
        song.finishLoading();
        return song;
    }

    /**
     * Adapts the song to the backend that will play it.
     * <p>
     * Only noteblocks are limited to 0..24 (a block has 25 states); the preview and portable
     * backends can play other pitches, so notes must not be dropped for them.
     */
    private static void fixSong(Song song, NotebotConfig config, Consumer<String> warn,
                                dev.notebot.playback.OutputBackend backend) {
        var iterator = song.getNotesMap().entries().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int tick = entry.getKey();
            Note note = entry.getValue();

            int level = note.getNoteLevel();
            if (level < 0 || level > 24) {
                switch (backend) {
                    case NOTEBLOCK -> {
                        if (config.transposeOutOfRange) {
                            note.setNoteLevel(foldIntoRange(level));
                        } else {
                            warn.accept("Note at tick " + tick + " is out of range for noteblocks, removed.");
                            iterator.remove();
                            continue;
                        }
                    }
                    // The audio backend clamps playback rate to 0.5..2.0 (OpenAL's AL_PITCH range),
                    // so keeping e.g. level 30 makes it sound as level 24 — audibly out of tune.
                    // /playsound has the same 0.5..2.0 limit. Fold by octaves in both cases.
                    case PORTABLE, PREVIEW -> note.setNoteLevel(foldIntoRange(level));
                }
            }

            // Instrument mapping is a noteblock concept: it decides which block plays a note.
            // Preview and portable should keep the instruments the song was written with.
            if (backend == dev.notebot.playback.OutputBackend.NOTEBLOCK) {
                if (config.usesExactInstruments()) {
                    NoteBlockInstrument mapped = config.mappedInstrument(note.getInstrument());
                    if (mapped != null) note.setInstrument(mapped);
                } else {
                    note.setInstrument(null);
                }
            }
        }
    }

    /** Moves a note into 0..24 by whole octaves. */
    private static int foldIntoRange(int level) {
        int value = level;
        while (value < 0) value += 12;
        while (value > 24) value -= 12;
        return value;
    }
}
