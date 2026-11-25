/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import net.minecraft.client.Minecraft;

/** Keeps vanilla background music out of the way while a song is playing. */
public final class VanillaMusic {
    private static int ticks;

    private VanillaMusic() {
    }

    /** Stops whatever vanilla music is playing right now (no-op when nothing plays). */
    public static void suppressNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getMusicManager() != null) {
            mc.getMusicManager().stopPlaying();
        }
    }

    /** Vanilla restarts music on its own schedule, so re-check periodically during playback. */
    public static void tickWhilePlaying() {
        if (++ticks % 20 == 0) {
            suppressNow();
        }
    }
}
