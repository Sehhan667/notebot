/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import dev.notebot.config.NotebotConfig;
import dev.notebot.song.Note;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.Collection;
import java.util.Locale;

/**
 * Plays notes with {@code /playsound} commands, so no noteblock is needed.
 * Requires permission level 2 and works only where the player may run commands.
 */
public final class PortableOutput implements NoteOutput {
    private final NotebotConfig config;
    private int commandsThisTick;

    public PortableOutput(NotebotConfig config) {
        this.config = config;
    }

    @Override
    public void onTick() {
        commandsThisTick = 0;
    }

    @Override
    public void playNotes(Collection<Note> notes) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || player.connection == null) return;

        String target = config.portableTarget == PortableTarget.NEARBY ? "@a" : "@s";
        float volume = Math.max(0.01f, config.portableVolume);
        float minVolume = Math.max(0.0f, Math.min(1.0f, config.portableMinVolume));

        int sent = 0;
        for (Note note : notes) {
            if (sent >= config.portableMaxCommandsPerTick) break;

            NoteBlockInstrument instrument = note.getInstrument();
            var sound = (instrument == null ? SoundEvents.NOTE_BLOCK_HARP : instrument.getSoundEvent())
                .value().location();

            float pitch = (float) Math.pow(2.0, (note.getNoteLevel() - 12) / 12.0);
            pitch = Math.max(0.5f, Math.min(2.0f, pitch));

            String command = String.format(Locale.ROOT,
                "playsound %s voice %s %.2f %.2f %.2f %.2f %.3f %.2f",
                sound, target,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch, minVolume);

            player.connection.sendCommand(command);
            sent++;
        }

        commandsThisTick += sent;
    }

    @Override
    public String status() {
        return "Portable";
    }
}
