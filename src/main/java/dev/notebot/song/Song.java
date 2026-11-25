/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.song;

import com.google.common.collect.Multimap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** A parsed song: tick -> notes, plus the unique note requirements used for block mapping. */
public final class Song {
    private final Multimap<Integer, Note> notesMap;
    private int lastTick;
    private final String title;
    private final String author;

    private final Set<Note> requirements = new HashSet<>();
    private boolean finishedLoading = false;

    public Song(Multimap<Integer, Note> notesMap, String title, String author) {
        this.notesMap = notesMap;
        this.title = title;
        this.author = author;
    }

    public void finishLoading() {
        if (finishedLoading) throw new IllegalStateException("Song has already finished loading!");
        if (notesMap.isEmpty()) {
            lastTick = 0;
        } else {
            lastTick = Collections.max(notesMap.keySet());
        }
        notesMap.values().stream().distinct().forEach(requirements::add);
        finishedLoading = true;
    }

    public Multimap<Integer, Note> getNotesMap() {
        return notesMap;
    }

    public Set<Note> getRequirements() {
        if (!finishedLoading) throw new IllegalStateException("Song is still loading!");
        return requirements;
    }

    public int getLastTick() {
        if (!finishedLoading) throw new IllegalStateException("Song is still loading!");
        return lastTick;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    /** Length in milliseconds, one tick being 50ms. */
    public long getLengthMillis() {
        return (long) lastTick * 50L;
    }
}
