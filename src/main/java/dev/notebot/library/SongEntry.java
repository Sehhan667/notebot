/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.library;

import java.nio.file.Path;

/** One song file found while scanning the library. */
public record SongEntry(String relativePath, String fileName, long size, long modified,
                        String title, String author) {
    /** Folder the song lives in, "" for the root. */
    public String folder() {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? "" : relativePath.substring(0, slash);
    }

    public String baseName() {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    /** Title from the file metadata, or the file name when the file has none. */
    public String displayTitle() {
        return title == null || title.isBlank() ? baseName() : title;
    }

    public Path resolve(Path root) {
        return root.resolve(relativePath);
    }
}
