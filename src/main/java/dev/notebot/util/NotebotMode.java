/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.util;

/** Whether songs keep their original instruments or may use any noteblock. */
public enum NotebotMode {
    /** Any noteblock can play any note, the instrument is ignored when tuning. */
    ANY_INSTRUMENT,
    /** Each note needs a noteblock whose instrument matches (after the note map is applied). */
    EXACT_INSTRUMENTS
}
