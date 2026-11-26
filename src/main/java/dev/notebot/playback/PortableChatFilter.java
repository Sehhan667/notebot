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
 * Keeps {@code /playsound} from spamming chat, and stops portable playback when the server
 * refuses the command (no permission / cheats disabled).
 */
public final class PortableChatFilter {
    private static final String OWN_PREFIX = "[notebot]";

    /** Guards against re-entering this listener from the warning we print ourselves. */
    private static boolean handling;

    private PortableChatFilter() {
    }

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (handling) return true;

            String plain = message.getString().toLowerCase(Locale.ROOT);

            // Our own "[Notebot] ..." lines must never be treated as server errors.
            if (plain.startsWith(OWN_PREFIX)) return true;

            String key = translationKey(message);

            if (key != null && key.startsWith("commands.playsound.success")) {
                return false;
            }

            // Without permission the server answers "Unknown or incomplete command",
            // so that key has to count as a rejection as well.
            boolean rejected = (key != null && (
                    key.toLowerCase(Locale.ROOT).contains("permission")
                        || key.equals("command.unknown.command")
                        || key.equals("command.unknown.argument")))
                || plain.contains("permission")
                || plain.contains("权限")
                || plain.contains("unknown or incomplete command")
                || plain.contains("未知或不完整的命令");

            if (rejected
                && NotebotMod.engine() != null
                && NotebotMod.engine().getActiveBackend() == OutputBackend.PORTABLE
                && PortableOutput.sentRecently()) {
                handling = true;
                try {
                    NotebotMod.engine().stop();
                    NotebotMod.warnToChat("Portable playback needs permission level 2 (op / cheats on). Stopped.");
                } finally {
                    handling = false;
                }
            }

            return true;
        });
    }

    private static String translationKey(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : null;
    }
}
