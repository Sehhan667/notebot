/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Shows a "now playing" toast, the same way vanilla announces a music disc. */
public final class NotebotToast {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(5000L);

    private NotebotToast() {
    }

    public static void nowPlaying(String title, String author) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;

        Component body = Component.literal(
            author == null || author.isBlank() ? title : title + " - " + author);

        SystemToast.add(mc.gui.toastManager(), TOAST_ID,
            Component.translatable("toast.notebot.now_playing"), body);
    }
}
