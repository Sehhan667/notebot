/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

/** Where the notes actually come from. */
public enum OutputBackend {
    /** Client-side {@code playSound}: instant, no world interaction, the "music player" mode. */
    PREVIEW,
    /** Tunes and hits real noteblocks with packets, like Meteor's Notebot. */
    NOTEBLOCK,
    /** Sends {@code /playsound} commands; needs permission level 2. */
    PORTABLE
}
