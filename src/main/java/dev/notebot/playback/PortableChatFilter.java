/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import dev.notebot.NotebotMod;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Locale;

/**
 * Keeps {@code /playsound} from spamming chat: success feedback is swallowed by translation key,
 * and a permission error stops portable playback instead of failing silently.
 */
public final class PortableChatFilter {
    private PortableChatFilter() {
    }

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String key = translationKey(message);

            if (key != null && key.startsWith("commands.playsound.success")) {
                return false;
            }

            String plain = message.getString().toLowerCase(Locale.ROOT);
            boolean permissionIssue = (key != null && key.toLowerCase(Locale.ROOT).contains("permission"))
                || plain.contains("permission")
                || plain.contains("权限");

            if (permissionIssue && NotebotMod.engine() != null
                && NotebotMod.engine().getActiveBackend() == OutputBackend.PORTABLE) {
                NotebotMod.engine().stop();
                NotebotMod.warnToChat("Portable playback needs permission level 2 (op / cheats on). Stopped.");
                return false;
            }

            return true;
        });
    }

    private static String translationKey(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : null;
    }
}
