/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.song;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads the simple Meteor text format:
 * {@code tick:noteLevel[:instrumentOrdinal]} per line, optional {@code // Name:} / {@code // Author:} headers.
 */
public final class TextSongDecoder extends SongDecoder {
    @Override
    public Song parse(File file, Consumer<String> warn) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        Multimap<Integer, Note> notesMap = MultimapBuilder.linkedHashKeys().arrayListValues().build();
        String title = FilenameUtils.getBaseName(file.getName());
        String author = "Unknown";

        NoteBlockInstrument[] instruments = NoteBlockInstrument.values();

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.startsWith("// Name: ")) {
                title = line.substring(9);
                continue;
            }
            if (line.startsWith("// Author: ")) {
                author = line.substring(11);
                continue;
            }
            if (line.isBlank()) continue;

            String[] parts = line.split(":");
            if (parts.length < 2) {
                warn.accept("Malformed line " + (lineNumber + 1));
                continue;
            }

            int tick;
            int level;
            int type = 0;
            try {
                tick = Integer.parseInt(parts[0].trim());
                level = Integer.parseInt(parts[1].trim());
                if (parts.length > 2) {
                    type = Integer.parseInt(parts[2].trim());
                }
            } catch (NumberFormatException e) {
                warn.accept("Invalid number on line " + (lineNumber + 1));
                continue;
            }

            if (type < 0 || type >= instruments.length) {
                warn.accept("Invalid instrument on line " + (lineNumber + 1));
                continue;
            }

            notesMap.put(tick, new Note(instruments[type], level));
        }

        return new Song(notesMap, title, author);
    }
}
