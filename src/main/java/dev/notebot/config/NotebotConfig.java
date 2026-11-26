/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.notebot.NotebotMod;
import dev.notebot.playback.OutputBackend;
import dev.notebot.playback.PlayMode;
import dev.notebot.playback.PortableTarget;
import dev.notebot.util.InstrumentDetectMode;
import dev.notebot.util.NotebotMode;
import dev.notebot.util.OptionalInstrument;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All persisted settings. Plain fields so Gson can round-trip them. */
public final class NotebotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NotebotConfig instance;

    // ---- General ----
    public int tickDelay = 1;
    public int concurrentTuneBlocks = 1;
    public NotebotMode mode = NotebotMode.EXACT_INSTRUMENTS;
    public InstrumentDetectMode instrumentDetectMode = InstrumentDetectMode.BLOCK_STATE;
    public boolean polyphonic = true;
    public boolean autoRotate = true;
    public boolean swingArm = true;
    public boolean transposeOutOfRange = false;
    public int checkNoteblocksAgainDelay = 10;
    /** How long a suspended song may be resumed after rejoining the world; 0 disables resuming. */
    public int resumeWindowMinutes = 10;

    // ---- Note map: vanilla instrument name -> OptionalInstrument name ----
    public Map<String, String> instruments = defaultInstrumentMap();

    // ---- Playback ----
    public PlayMode playMode = PlayMode.SEQUENTIAL;
    public OutputBackend backend = OutputBackend.PREVIEW;
    public float volume = 1.0f;

    // ---- Portable (/playsound) backend ----
    public PortableTarget portableTarget = PortableTarget.NEARBY;
    public float portableVolume = 1.0f;
    public float portableMinVolume = 0.0f;
    public int portableMaxCommandsPerTick = 8;

    // ---- Library ----
    public List<String> favorites = new ArrayList<>();

    // =====================================================================

    private static Map<String, String> defaultInstrumentMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (OptionalInstrument instrument : OptionalInstrument.values()) {
            if (instrument != OptionalInstrument.NONE) {
                map.put(instrument.name(), instrument.name());
            }
        }
        return map;
    }

    public static NotebotConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("notebot.json");
    }

    public static synchronized void load() {
        Path path = configPath();
        NotebotConfig loaded = null;
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, NotebotConfig.class);
            } catch (Exception e) {
                NotebotMod.LOG.error("Failed to read {}, falling back to defaults", path, e);
            }
        }

        if (loaded == null) loaded = new NotebotConfig();
        loaded.sanitize();
        instance = loaded;
        instance.save();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(configPath().getParent());
            try (Writer writer = Files.newBufferedWriter(configPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            NotebotMod.LOG.error("Failed to save {}", configPath(), e);
        }
    }

    /** Repairs values that could come from a hand-edited or older file. */
    private void sanitize() {
        tickDelay = clamp(tickDelay, 1, 20);
        concurrentTuneBlocks = clamp(concurrentTuneBlocks, 1, 20);
        checkNoteblocksAgainDelay = clamp(checkNoteblocksAgainDelay, 1, 20);
        resumeWindowMinutes = clamp(resumeWindowMinutes, 0, 120);
        portableMaxCommandsPerTick = clamp(portableMaxCommandsPerTick, 1, 32);
        volume = clampFloat(volume, 0.0f, 4.0f);
        portableVolume = clampFloat(portableVolume, 0.0f, 4.0f);
        portableMinVolume = clampFloat(portableMinVolume, 0.0f, 1.0f);
        if (mode == null) mode = NotebotMode.EXACT_INSTRUMENTS;
        if (instrumentDetectMode == null) instrumentDetectMode = InstrumentDetectMode.BLOCK_STATE;
        if (playMode == null) playMode = PlayMode.SEQUENTIAL;
        if (backend == null) backend = OutputBackend.PREVIEW;
        if (portableTarget == null) portableTarget = PortableTarget.NEARBY;
        if (favorites == null) favorites = new ArrayList<>();
        if (instruments == null) instruments = defaultInstrumentMap();

        // Fill in any instrument missing from an older config.
        for (Map.Entry<String, String> entry : defaultInstrumentMap().entrySet()) {
            instruments.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // =====================================================================

    public boolean usesExactInstruments() {
        return mode == NotebotMode.EXACT_INSTRUMENTS;
    }

    public OptionalInstrument optionalInstrument(NoteBlockInstrument instrument) {
        if (instrument == null) return OptionalInstrument.NONE;
        String value = instruments.get(instrument.name());
        if (value == null) return OptionalInstrument.fromMinecraftInstrument(instrument);
        try {
            return OptionalInstrument.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OptionalInstrument.fromMinecraftInstrument(instrument);
        }
    }

    /** The instrument this song instrument should be played with, or {@code null} for "any". */
    public NoteBlockInstrument mappedInstrument(NoteBlockInstrument instrument) {
        return optionalInstrument(instrument).toMinecraftInstrument();
    }

    public void setMappedInstrument(NoteBlockInstrument from, OptionalInstrument to) {
        instruments.put(from.name(), to.name());
    }

    // ---- Favourites ----

    public boolean isFavorite(String relativePath) {
        return favorites.contains(relativePath);
    }

    public void setFavorite(String relativePath, boolean favorite) {
        if (favorite) {
            if (!favorites.contains(relativePath)) favorites.add(relativePath);
        } else {
            favorites.remove(relativePath);
        }
    }

    /** Drops favourites whose file no longer exists. Returns how many were removed. */
    public int pruneFavorites(Path root) {
        int before = favorites.size();
        favorites.removeIf(relative -> !Files.isRegularFile(root.resolve(relative)));
        return before - favorites.size();
    }
}
