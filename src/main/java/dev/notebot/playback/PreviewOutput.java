/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import dev.notebot.config.NotebotConfig;
import dev.notebot.song.Note;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.Collection;

/** Plays notes locally on the client: no blocks, no packets, no server involvement. */
public final class PreviewOutput implements NoteOutput {
    private final NotebotConfig config;

    public PreviewOutput(NotebotConfig config) {
        this.config = config;
    }

    @Override
    public void playNotes(Collection<Note> notes) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        for (Note note : notes) {
            NoteBlockInstrument instrument = note.getInstrument();

            SoundEvent sound = instrument == null
                ? SoundEvents.NOTE_BLOCK_HARP.value()
                : instrument.getSoundEvent().value();

            float pitch = (float) Math.pow(2.0, (note.getNoteLevel() - 12) / 12.0);

            // Volume above 1 only widens the audible radius in vanilla; keep it in a sane range.
            player.playSound(sound, Math.max(0.0f, config.volume), pitch);
        }
    }

    @Override
    public String status() {
        return "Preview";
    }
}
