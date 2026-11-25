/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

/** Who receives the sound in portable mode. */
public enum PortableTarget {
    /** {@code @a} positioned at the player: nearby players hear it, with distance falloff. */
    NEARBY,
    /** {@code @s}: only the player hears it. */
    SELF
}
