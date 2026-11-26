/*
 * Notebot — a standalone port of MeteorClient's Notebot for Minecraft 26.3.
 * Derived from Meteor Client (https://github.com/MeteorDevelopment/meteor-client), GPL-3.0.
 */

package dev.notebot;

import dev.notebot.config.NotebotConfig;
import dev.notebot.library.SongLibrary;
import dev.notebot.playback.PlaybackEngine;
import dev.notebot.playback.PortableChatFilter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public final class NotebotMod implements ClientModInitializer {
    public static final String MOD_ID = "notebot";
    public static final Logger LOG = LoggerFactory.getLogger("Notebot");

    private static NotebotConfig config;
    private static SongLibrary library;
    private static PlaybackEngine engine;

    public static NotebotConfig config() {
        return config;
    }

    public static SongLibrary library() {
        return library;
    }

    public static PlaybackEngine engine() {
        return engine;
    }

    @Override
    public void onInitializeClient() {
        config = NotebotConfig.get();

        var root = FabricLoader.getInstance().getGameDir().resolve("notebot");
        library = new SongLibrary(root);
        library.ensureRoot();
        engine = new PlaybackEngine(config, library);

        ClientTickEvents.END_CLIENT_TICK.register(client -> engine.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> engine.suspend());
        Keybinds.init();
        PortableChatFilter.init();

        // Index the library off-thread so opening the screen is instant.
        CompletableFuture.runAsync(library::rescan);

        LOG.info("Notebot initialised for Minecraft 26.3");
    }

    /** Prefixes a message with [Notebot] and puts it in the chat HUD. */
    public static void warnToChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("[Notebot] " + message));
        } else {
            LOG.info("[Notebot] {}", message);
        }
    }
}
