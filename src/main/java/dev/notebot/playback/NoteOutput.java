/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import dev.notebot.song.Note;
import dev.notebot.song.Song;

import java.util.Collection;

/**
 * Sink for the notes of one tick. Implementations differ in how they make the sound happen;
 * everything else (queue, seek, looping, progress) lives in {@link PlaybackEngine}.
 */
public interface NoteOutput {
    /** Called once when a song starts. Backends that need preparation do it here. */
    default void onStart(String relativePath, Song song) {
    }

    /** Called when playback stops or another song starts. */
    default void onStop() {
    }

    /** Called every client tick while preparing and while playing. */
    default void onTick() {
    }

    /** Emits the notes scheduled for the current tick. */
    void playNotes(Collection<Note> notes);

    /** False while the backend still prepares (e.g. tuning noteblocks). */
    default boolean ready() {
        return true;
    }

    /** True when the backend cannot continue (no blocks in reach, missing permission, ...). */
    default boolean failed() {
        return false;
    }

    /** Short human readable state, shown next to the progress bar. */
    default String status() {
        return "";
    }
}
