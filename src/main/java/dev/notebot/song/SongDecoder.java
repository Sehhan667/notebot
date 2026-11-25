/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.song;

import java.io.File;
import java.util.function.Consumer;

/** Parses a song file into a {@link Song}. */
public abstract class SongDecoder {
    public abstract Song parse(File file, Consumer<String> warn) throws Exception;
}
