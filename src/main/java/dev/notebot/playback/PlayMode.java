/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

/** What happens when the current song ends. */
public enum PlayMode {
    /** Play the queue once and stop. */
    SEQUENTIAL,
    /** Loop the whole queue. */
    LOOP_ALL,
    /** Repeat the current song. */
    LOOP_ONE,
    /** Jump to a random entry of the queue. */
    SHUFFLE
}
