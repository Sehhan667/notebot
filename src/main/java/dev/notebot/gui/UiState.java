/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

/** Survives screen switches so the library keeps its folder, filter and scroll position. */
public final class UiState {
    public static String folder = "";
    public static String filter = "";
    public static boolean favoritesOnly;
    public static int scroll;

    private UiState() {
    }
}
