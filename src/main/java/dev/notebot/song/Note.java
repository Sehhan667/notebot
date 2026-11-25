/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.song;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.Objects;

/** A single required note: an instrument plus a note level (0-24). */
public final class Note {
    private NoteBlockInstrument instrument;
    private int noteLevel;

    public Note(NoteBlockInstrument instrument, int noteLevel) {
        this.instrument = instrument;
        this.noteLevel = noteLevel;
    }

    public NoteBlockInstrument getInstrument() {
        return instrument;
    }

    public void setInstrument(NoteBlockInstrument instrument) {
        this.instrument = instrument;
    }

    public int getNoteLevel() {
        return noteLevel;
    }

    public void setNoteLevel(int noteLevel) {
        this.noteLevel = noteLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note note)) return false;
        return instrument == note.instrument && noteLevel == note.noteLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrument, noteLevel);
    }

    @Override
    public String toString() {
        return "Note{instrument=" + instrument + ", noteLevel=" + noteLevel + '}';
    }
}
