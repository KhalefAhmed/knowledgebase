package example;

import java.util.ArrayList;
import java.util.List;

final class TextChunker {
    static final int MAX_CHUNK_LENGTH = 1200;

    private TextChunker() {
    }

    static List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : text.split("\\R\\s*\\R")) {
            String normalized = paragraph.trim();
            if (normalized.isEmpty()) {
                continue;
            }

            for (String part : splitLongParagraph(normalized)) {
                int separatorLength = current.isEmpty() ? 0 : 2;
                if (current.length() + separatorLength + part.length() > MAX_CHUNK_LENGTH) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                if (!current.isEmpty()) {
                    current.append("\n\n");
                }
                current.append(part);
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return List.copyOf(chunks);
    }

    private static List<String> splitLongParagraph(String paragraph) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, paragraph.length());
            if (end < paragraph.length()) {
                int boundary = paragraph.lastIndexOf(' ', end - 1);
                if (boundary > start) {
                    end = boundary + 1;
                }
            }
            parts.add(paragraph.substring(start, end));
            start = end;
        }
        return parts;
    }
}
