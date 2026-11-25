/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.util;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.HashMap;
import java.util.Map;

/** Mappable instrument for the "Note Map" settings: "no instrument" plus every vanilla instrument. */
public enum OptionalInstrument {
    NONE(null),
    HARP(NoteBlockInstrument.HARP),
    BASEDRUM(NoteBlockInstrument.BASEDRUM),
    SNARE(NoteBlockInstrument.SNARE),
    HAT(NoteBlockInstrument.HAT),
    BASS(NoteBlockInstrument.BASS),
    FLUTE(NoteBlockInstrument.FLUTE),
    BELL(NoteBlockInstrument.BELL),
    GUITAR(NoteBlockInstrument.GUITAR),
    CHIME(NoteBlockInstrument.CHIME),
    XYLOPHONE(NoteBlockInstrument.XYLOPHONE),
    IRON_XYLOPHONE(NoteBlockInstrument.IRON_XYLOPHONE),
    COW_BELL(NoteBlockInstrument.COW_BELL),
    DIDGERIDOO(NoteBlockInstrument.DIDGERIDOO),
    BIT(NoteBlockInstrument.BIT),
    BANJO(NoteBlockInstrument.BANJO),
    PLING(NoteBlockInstrument.PLING),
    // Instruments added by 26.3 (mob heads, trumpets).
    TRUMPET(NoteBlockInstrument.TRUMPET),
    TRUMPET_EXPOSED(NoteBlockInstrument.TRUMPET_EXPOSED),
    TRUMPET_OXIDIZED(NoteBlockInstrument.TRUMPET_OXIDIZED),
    TRUMPET_WEATHERED(NoteBlockInstrument.TRUMPET_WEATHERED),
    ZOMBIE(NoteBlockInstrument.ZOMBIE),
    SKELETON(NoteBlockInstrument.SKELETON),
    CREEPER(NoteBlockInstrument.CREEPER),
    DRAGON(NoteBlockInstrument.DRAGON),
    WITHER_SKELETON(NoteBlockInstrument.WITHER_SKELETON),
    PIGLIN(NoteBlockInstrument.PIGLIN),
    CUSTOM_HEAD(NoteBlockInstrument.CUSTOM_HEAD);

    public static final Map<NoteBlockInstrument, OptionalInstrument> BY_MINECRAFT_INSTRUMENT = new HashMap<>();

    static {
        for (OptionalInstrument instrument : values()) {
            if (instrument.minecraftInstrument != null) {
                BY_MINECRAFT_INSTRUMENT.put(instrument.minecraftInstrument, instrument);
            }
        }
    }

    private final NoteBlockInstrument minecraftInstrument;

    OptionalInstrument(NoteBlockInstrument minecraftInstrument) {
        this.minecraftInstrument = minecraftInstrument;
    }

    public NoteBlockInstrument toMinecraftInstrument() {
        return minecraftInstrument;
    }

    public static OptionalInstrument fromMinecraftInstrument(NoteBlockInstrument instrument) {
        if (instrument == null) return NONE;
        return BY_MINECRAFT_INSTRUMENT.getOrDefault(instrument, NONE);
    }
}
