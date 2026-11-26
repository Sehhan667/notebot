/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Maps the English literals used by the screens to translation keys.
 * Unknown strings (song titles, paths, numbers) fall through unchanged.
 */
public final class Lang {
    private static final Map<String, String> UI = Map.ofEntries(
        // Chrome
        Map.entry("Notebot", "notebot.ui.title"),
        Map.entry("Notebot Settings", "notebot.ui.settings_title"),
        Map.entry("General", "notebot.ui.tab_general"),
        Map.entry("Note map", "notebot.ui.tab_note_map"),
        Map.entry("Done", "notebot.ui.done"),
        Map.entry("Back", "notebot.ui.back"),
        Map.entry("Player", "notebot.ui.player"),
        Map.entry("Settings", "notebot.ui.settings"),
        Map.entry("Reload", "notebot.ui.reload"),
        Map.entry("Up", "notebot.ui.up"),
        Map.entry("Favourites", "notebot.ui.favourites"),
        Map.entry("Favourites: on", "notebot.ui.favourites_on"),
        Map.entry("Search songs…", "notebot.ui.search"),
        Map.entry("No songs found", "notebot.ui.no_songs"),
        Map.entry("Scanning…", "notebot.ui.scanning"),
        Map.entry("No song loaded", "notebot.ui.no_song"),
        Map.entry("Prev", "notebot.ui.prev"),
        Map.entry("Pause", "notebot.ui.pause"),
        Map.entry("Play", "notebot.ui.play"),
        Map.entry("Next", "notebot.ui.next"),
        Map.entry("Stop", "notebot.ui.stop"),
        Map.entry("On", "notebot.ui.on"),
        Map.entry("Off", "notebot.ui.off"),
        Map.entry("Volume (preview)", "notebot.ui.volume_preview"),
        Map.entry("Volume (portable)", "notebot.ui.volume_portable"),
        Map.entry("Volume: fixed by the game (note blocks)", "notebot.ui.volume_fixed"),
        Map.entry("Output", "notebot.ui.output"),

        // Settings
        Map.entry("Tick delay", "notebot.setting.tick_delay"),
        Map.entry("Concurrent tune blocks", "notebot.setting.concurrent"),
        Map.entry("Mode", "notebot.setting.mode"),
        Map.entry("Instrument detect", "notebot.setting.instrument_detect"),
        Map.entry("Polyphonic", "notebot.setting.polyphonic"),
        Map.entry("Auto rotate", "notebot.setting.auto_rotate"),
        Map.entry("Swing arm", "notebot.setting.swing_arm"),
        Map.entry("Transpose out of range", "notebot.setting.transpose"),
        Map.entry("Re-check delay", "notebot.setting.recheck"),
        Map.entry("Resume window (min)", "notebot.setting.resume_window"),
        Map.entry("Portable output (/playsound)", "notebot.setting.portable_header"),
        Map.entry("Target", "notebot.setting.target"),
        Map.entry("Volume %", "notebot.setting.portable_volume"),
        Map.entry("Min volume %", "notebot.setting.min_volume"),
        Map.entry("Max commands / tick", "notebot.setting.max_commands"),
        Map.entry("Maps a song instrument to the instrument actually played", "notebot.setting.note_map_hint"),

        // Status
        Map.entry("Preparing", "notebot.status.preparing"),
        Map.entry("Playing", "notebot.status.playing"),
        Map.entry("Paused", "notebot.status.paused"),
        Map.entry("Preview", "notebot.status.preview"),
        Map.entry("Portable", "notebot.status.portable"),
        Map.entry("Noteblock", "notebot.status.noteblock"),
        Map.entry("Scanning noteblocks", "notebot.status.scanning"),
        Map.entry("Tuning", "notebot.status.tuning"),
        Map.entry("Checking noteblocks", "notebot.status.checking"),
        Map.entry("Re-tuning", "notebot.status.retuning"),

        // Enums
        Map.entry("Sequential", "notebot.enum.sequential"),
        Map.entry("Loop all", "notebot.enum.loop_all"),
        Map.entry("Loop one", "notebot.enum.loop_one"),
        Map.entry("Shuffle", "notebot.enum.shuffle"),
        Map.entry("Exact instruments", "notebot.enum.exact"),
        Map.entry("Any instrument", "notebot.enum.any"),
        Map.entry("Block state", "notebot.enum.block_state"),
        Map.entry("Below block", "notebot.enum.below_block"),
        Map.entry("Nearby", "notebot.enum.nearby"),
        Map.entry("Self", "notebot.enum.self"),
        Map.entry("None", "notebot.enum.none"),

        // Instruments
        Map.entry("Harp", "notebot.instrument.harp"),
        Map.entry("Basedrum", "notebot.instrument.basedrum"),
        Map.entry("Snare", "notebot.instrument.snare"),
        Map.entry("Hat", "notebot.instrument.hat"),
        Map.entry("Bass", "notebot.instrument.bass"),
        Map.entry("Flute", "notebot.instrument.flute"),
        Map.entry("Bell", "notebot.instrument.bell"),
        Map.entry("Guitar", "notebot.instrument.guitar"),
        Map.entry("Chime", "notebot.instrument.chime"),
        Map.entry("Xylophone", "notebot.instrument.xylophone"),
        Map.entry("Iron xylophone", "notebot.instrument.iron_xylophone"),
        Map.entry("Cow bell", "notebot.instrument.cow_bell"),
        Map.entry("Didgeridoo", "notebot.instrument.didgeridoo"),
        Map.entry("Bit", "notebot.instrument.bit"),
        Map.entry("Banjo", "notebot.instrument.banjo"),
        Map.entry("Pling", "notebot.instrument.pling"),
        Map.entry("Trumpet", "notebot.instrument.trumpet"),
        Map.entry("Trumpet exposed", "notebot.instrument.trumpet_exposed"),
        Map.entry("Trumpet oxidized", "notebot.instrument.trumpet_oxidized"),
        Map.entry("Trumpet weathered", "notebot.instrument.trumpet_weathered"),
        Map.entry("Zombie", "notebot.instrument.zombie"),
        Map.entry("Skeleton", "notebot.instrument.skeleton"),
        Map.entry("Creeper", "notebot.instrument.creeper"),
        Map.entry("Dragon", "notebot.instrument.dragon"),
        Map.entry("Wither skeleton", "notebot.instrument.wither_skeleton"),
        Map.entry("Piglin", "notebot.instrument.piglin"),
        Map.entry("Custom head", "notebot.instrument.custom_head")
    );

    private Lang() {
    }

    /** Translates a known UI literal, or returns it verbatim when it is dynamic content. */
    public static Component ui(String english) {
        String key = UI.get(english);
        return key == null ? Component.literal(english) : Component.translatableWithFallback(key, english);
    }

    /** Same lookup but as a plain string, for labels that get concatenated. */
    public static String text(String english) {
        String key = UI.get(english);
        return key == null ? english : Component.translatableWithFallback(key, english).getString();
    }
}
