/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 * Original NBS decoding logic based on NoteBlockAPI's NBSDecoder (https://opennbs.org/nbs).
 */

package dev.notebot.song;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/** Reads Note Block Studio projects (.nbs). */
public final class NBSSongDecoder extends SongDecoder {
    /** Magic offset between NBS keys and vanilla note levels. */
    public static final int NOTE_OFFSET = 33;

    @Override
    public Song parse(File file, Consumer<String> warn) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return parse(in);
        }
    }

    private Song parse(InputStream inputStream) throws Exception {
        Multimap<Integer, Note> notesMap = MultimapBuilder.linkedHashKeys().arrayListValues().build();

        DataInputStream in = new DataInputStream(inputStream);
        short length = readShort(in);
        int nbsVersion = 0;
        if (length == 0) {
            nbsVersion = in.readByte();
            in.readByte(); // first custom instrument
            if (nbsVersion >= 3) {
                length = readShort(in);
            }
        }
        readShort(in); // song height
        String title = readString(in);
        String author = readString(in);
        readString(in); // original author
        readString(in); // description
        float speed = readShort(in) / 100f;
        in.readBoolean(); // auto-save
        in.readByte();    // auto-save duration
        in.readByte();    // time signature
        readInt(in);      // minutes spent
        readInt(in);      // left clicks
        readInt(in);      // right clicks
        readInt(in);      // blocks added
        readInt(in);      // blocks removed
        readString(in);   // midi/schematic name
        if (nbsVersion >= 4) {
            in.readByte();  // loop on/off
            in.readByte();  // max loop count
            readShort(in);  // loop start tick
        }

        double tick = -1;
        while (true) {
            short jumpTicks = readShort(in); // ticks until the next column
            if (jumpTicks == 0) {
                break;
            }
            tick += jumpTicks * (20f / speed);

            while (true) {
                short jumpLayers = readShort(in); // layers until the next note
                if (jumpLayers == 0) {
                    break;
                }
                byte instrument = in.readByte();
                byte key = in.readByte();
                if (nbsVersion >= 4) {
                    in.readUnsignedByte(); // velocity
                    in.readUnsignedByte(); // panning
                    readShort(in);         // pitch
                }

                NoteBlockInstrument inst = fromNbsInstrument(instrument);
                // Custom instrument: skip the note, vanilla cannot play it.
                if (inst == null) continue;

                notesMap.put((int) Math.round(tick), new Note(inst, key - NOTE_OFFSET));
            }
        }

        return new Song(notesMap, title, author);
    }

    private static short readShort(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        return (short) (b1 + (b2 << 8));
    }

    private static int readInt(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        int b4 = in.readUnsignedByte();
        return b1 + (b2 << 8) + (b3 << 16) + (b4 << 24);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readInt(in);
        if (length < 0) {
            throw new EOFException("Negative string length: " + length);
        }
        if (length > in.available()) {
            throw new EOFException("String longer than remaining buffer: " + length + " > " + in.available());
        }

        StringBuilder sb = new StringBuilder(length);
        for (; length > 0; --length) {
            char c = (char) in.readByte();
            if (c == '\r') c = ' ';
            sb.append(c);
        }
        return sb.toString();
    }

    /** Maps NBS instrument ids to vanilla instruments; unknown ids are custom instruments. */
    private static NoteBlockInstrument fromNbsInstrument(int instrument) {
        return switch (instrument) {
            case 0 -> NoteBlockInstrument.HARP;
            case 1 -> NoteBlockInstrument.BASS;
            case 2 -> NoteBlockInstrument.BASEDRUM;
            case 3 -> NoteBlockInstrument.SNARE;
            case 4 -> NoteBlockInstrument.HAT;
            case 5 -> NoteBlockInstrument.GUITAR;
            case 6 -> NoteBlockInstrument.FLUTE;
            case 7 -> NoteBlockInstrument.BELL;
            case 8 -> NoteBlockInstrument.CHIME;
            case 9 -> NoteBlockInstrument.XYLOPHONE;
            case 10 -> NoteBlockInstrument.IRON_XYLOPHONE;
            case 11 -> NoteBlockInstrument.COW_BELL;
            case 12 -> NoteBlockInstrument.DIDGERIDOO;
            case 13 -> NoteBlockInstrument.BIT;
            case 14 -> NoteBlockInstrument.BANJO;
            case 15 -> NoteBlockInstrument.PLING;
            default -> null;
        };
    }
}
