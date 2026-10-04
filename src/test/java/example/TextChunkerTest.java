package example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void splitsParagraphsIntoBoundedChunks() {
        String paragraph = "word ".repeat(TextChunker.MAX_CHUNK_LENGTH / 5 + 100);

        var chunks = TextChunker.split(paragraph);

        assertEquals(2, chunks.size());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= TextChunker.MAX_CHUNK_LENGTH));
        assertEquals(paragraph.trim(), String.join("", chunks));
    }

    @Test
    void combinesShortParagraphsWithoutExceedingTheLimit() {
        var chunks = TextChunker.split("First paragraph.\n\nSecond paragraph.");

        assertEquals(1, chunks.size());
        assertEquals("First paragraph.\n\nSecond paragraph.", chunks.getFirst());
    }
}
