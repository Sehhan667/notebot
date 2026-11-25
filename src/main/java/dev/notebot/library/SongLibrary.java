/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.library;

import dev.notebot.NotebotMod;
import dev.notebot.config.NotebotConfig;
import dev.notebot.song.SongDecoders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Recursive index of the {@code .minecraft/notebot} folder.
 * The scan itself is cheap enough to run on a worker thread whenever the library screen opens.
 */
public final class SongLibrary {
    private final Path root;
    private final List<SongEntry> songs = new ArrayList<>();

    public SongLibrary(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    public void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            NotebotMod.LOG.error("Could not create song folder {}", root, e);
        }
    }

    /** Rebuilds the index. Blocking; call from a worker thread. */
    public synchronized void rescan() {
        songs.clear();
        if (!Files.isDirectory(root)) return;

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                .filter(SongDecoders::hasDecoder)
                .forEach(file -> {
                    try {
                        String relative = root.relativize(file).toString().replace('\\', '/');
                        SongMetadata.Info info = SongMetadata.read(file);
                        songs.add(new SongEntry(
                            relative,
                            file.getFileName().toString(),
                            Files.size(file),
                            Files.getLastModifiedTime(file).toMillis(),
                            info == null ? null : info.title(),
                            info == null ? null : info.author()
                        ));
                    } catch (IOException ignored) {
                        // File vanished mid-scan; skip it.
                    }
                });
        } catch (IOException e) {
            NotebotMod.LOG.error("Failed to scan song folder {}", root, e);
        }

        songs.sort((a, b) -> a.relativePath().compareToIgnoreCase(b.relativePath()));
    }

    public synchronized List<SongEntry> all() {
        return List.copyOf(songs);
    }

    /** Songs directly inside {@code folder}, or in its whole subtree when {@code recursive}. */
    public synchronized List<SongEntry> songsIn(String folder, boolean recursive) {
        String prefix = folder.isEmpty() ? "" : folder + "/";
        List<SongEntry> result = new ArrayList<>();
        for (SongEntry entry : songs) {
            if (!entry.relativePath().startsWith(prefix)) continue;
            if (!recursive) {
                String rest = entry.relativePath().substring(prefix.length());
                if (rest.contains("/")) continue;
            }
            result.add(entry);
        }
        return result;
    }

    /** Immediate sub-folders of {@code folder} that contain at least one song. */
    public synchronized List<String> subFolders(String folder) {
        String prefix = folder.isEmpty() ? "" : folder + "/";
        Set<String> names = new HashSet<>();
        for (SongEntry entry : songs) {
            if (!entry.relativePath().startsWith(prefix)) continue;
            String rest = entry.relativePath().substring(prefix.length());
            int slash = rest.indexOf('/');
            if (slash > 0) names.add(rest.substring(0, slash));
        }
        List<String> result = new ArrayList<>(names);
        result.sort(String::compareToIgnoreCase);
        return result;
    }

    /** Case-insensitive search over the file name, its folder path, and the song's own title/author. */
    public synchronized List<SongEntry> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT).trim();
        if (needle.isEmpty()) return all();
        List<SongEntry> result = new ArrayList<>();
        for (SongEntry entry : songs) {
            if (contains(entry.relativePath(), needle)
                || contains(entry.title(), needle)
                || contains(entry.author(), needle)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    public synchronized List<SongEntry> favorites() {
        NotebotConfig config = NotebotConfig.get();
        List<SongEntry> result = new ArrayList<>();
        for (SongEntry entry : songs) {
            if (config.isFavorite(entry.relativePath())) result.add(entry);
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    public synchronized SongEntry find(String relativePath) {
        for (SongEntry entry : songs) {
            if (entry.relativePath().equals(relativePath)) return entry;
        }
        return null;
    }
}
