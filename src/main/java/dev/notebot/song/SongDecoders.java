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
    public static Song parse(File file, NotebotConfig config, Consumer<String> warn) throws Exception {
        SongDecoder decoder = DECODERS.get(extension(file.getName()));
        if (decoder == null) throw new IllegalStateException("No decoder for " + file.getName());

        Song song = decoder.parse(file, warn);
        fixSong(song, config, warn);
        song.finishLoading();
        return song;
    }

    private static void fixSong(Song song, NotebotConfig config, Consumer<String> warn) {
        var iterator = song.getNotesMap().entries().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int tick = entry.getKey();
            Note note = entry.getValue();

            int level = note.getNoteLevel();
            if (level < 0 || level > 24) {
                if (config.transposeOutOfRange) {
                    while (level < 0) level += 12;
                    while (level > 24) level -= 12;
                    note.setNoteLevel(level);
                } else {
                    warn.accept("Note at tick " + tick + " is out of range, removed.");
                    iterator.remove();
                    continue;
                }
            }

            if (config.usesExactInstruments()) {
                NoteBlockInstrument mapped = config.mappedInstrument(note.getInstrument());
                if (mapped != null) note.setInstrument(mapped);
            } else {
                note.setInstrument(null);
            }
        }
    }
}
