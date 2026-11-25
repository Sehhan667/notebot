/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.library;

/** Reads only the title/author header of a song file, without parsing the notes. */
public final class SongMetadata {
    public record Info(String title, String author) {
    }

    private SongMetadata() {
    }

    public static Info read(java.nio.file.Path path) {
        String fileName = path.getFileName().toString();
        String extension = dev.notebot.song.SongDecoders.extension(fileName);
        try {
            return switch (extension) {
                case "nbs" -> readNbs(path);
                case "txt" -> readTxt(path);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static Info readNbs(java.nio.file.Path path) throws Exception {
        try (var in = new java.io.DataInputStream(java.nio.file.Files.newInputStream(path))) {
            short length = readShort(in);
            int version = 0;
            if (length == 0) {
                version = in.readByte();
                in.readByte(); // first custom instrument
                if (version >= 3) readShort(in);
            }
            readShort(in); // song height
            String title = readString(in);
            String author = readString(in);
            return new Info(title, author);
        }
    }

    private static Info readTxt(java.nio.file.Path path) throws Exception {
        String title = null;
        String author = null;
        try (var lines = java.nio.file.Files.lines(path)) {
            var iterator = lines.iterator();
            int read = 0;
            while (iterator.hasNext() && read++ < 20) {
                String line = iterator.next();
                if (line.startsWith("// Name: ")) title = line.substring(9).trim();
                else if (line.startsWith("// Author: ")) author = line.substring(11).trim();
                else if (!line.isBlank() && !line.startsWith("//")) break;
            }
        }
        return new Info(title, author);
    }

    private static short readShort(java.io.DataInputStream in) throws java.io.IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        return (short) (b1 + (b2 << 8));
    }

    private static int readInt(java.io.DataInputStream in) throws java.io.IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        int b4 = in.readUnsignedByte();
        return b1 + (b2 << 8) + (b3 << 16) + (b4 << 24);
    }

    private static String readString(java.io.DataInputStream in) throws java.io.IOException {
        int length = readInt(in);
        if (length < 0 || length > in.available()) return "";
        StringBuilder sb = new StringBuilder(length);
        for (; length > 0; --length) {
            char c = (char) in.readByte();
            if (c == '\r') c = ' ';
            sb.append(c);
        }
        return sb.toString();
    }
}
