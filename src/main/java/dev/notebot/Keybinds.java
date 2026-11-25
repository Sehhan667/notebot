/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot;

import com.mojang.blaze3d.platform.InputConstants;
import dev.notebot.gui.NotebotScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** The single hotkey that opens the Notebot menu; unbound by default so it never fights Vanilla keys. */
public final class Keybinds {
    /** Own category so the entry is easy to find in Controls instead of hiding inside "Miscellaneous". */
    public static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NotebotMod.MOD_ID, "main"));

    public static final KeyMapping OPEN_MENU = new KeyMapping(
        "key.notebot.open",
        InputConstants.Type.KEYBOARD,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );

    private Keybinds() {
    }

    public static void init() {
        KeyMappingHelper.registerKeyMapping(OPEN_MENU);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new NotebotScreen());
                }
            }
        });
    }
}
