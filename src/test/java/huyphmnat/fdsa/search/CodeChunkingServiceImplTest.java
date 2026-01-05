package huyphmnat.fdsa.search;

import huyphmnat.fdsa.search.internal.services.CodeChunkingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeChunkingServiceImplTest {

    private CodeChunkingServiceImpl chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new CodeChunkingServiceImpl();
    }

    @Test
    void testChunkCode_SmallCode_ShouldReturnSingleChunk() {
        String code = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}";

        List<String> chunks = chunkingService.chunkCode(code);

        assertEquals(1, chunks.size());
        assertEquals(code, chunks.get(0));
    }

    @Test
    void testChunkCode_EmptyCode_ShouldReturnSingleEmptyChunk() {
        String code = "";

        List<String> chunks = chunkingService.chunkCode(code);

        assertEquals(1, chunks.size());
        assertEquals("", chunks.get(0));
    }

    @Test
    void testChunkCode_LargeCode_ShouldSplitIntoMultipleChunks() {
        // Create code larger than chunk size (512 tokens * 4 chars = 2048 chars)
        StringBuilder largeCode = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeCode.append("public class Class").append(i).append(" {\n");
            largeCode.append("    private String field").append(i).append(";\n");
            largeCode.append("    public void method").append(i).append("() { }\n");
            largeCode.append("}\n\n");
        }

        List<String> chunks = chunkingService.chunkCode(largeCode.toString());

        assertTrue(chunks.size() > 1, "Large code should be split into multiple chunks");

        // Verify each chunk is not empty
        for (String chunk : chunks) {
            assertFalse(chunk.isEmpty());
        }

        // Verify chunks when concatenated approximately equal original (may have trailing newlines)
        String concatenated = String.join("", chunks);
        assertTrue(concatenated.length() >= largeCode.length() - chunks.size());
    }

    @Test
    void testChunkCode_SplitsOnLineBreaks() {
        // Create code with specific line structure
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            code.append("// This is line ").append(i).append(" with some content to make it reasonably long\n");
        }

        List<String> chunks = chunkingService.chunkCode(code.toString());

        // Each chunk should end with newline (except possibly the last one if original didn't)
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertTrue(chunks.get(i).endsWith("\n"), "Chunk should end with newline");
        }
    }

    @Test
    void testChunkCode_SingleLongLine_ShouldStillChunk() {
        // Create a single very long line without line breaks
        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longLine.append("code ");
        }

        List<String> chunks = chunkingService.chunkCode(longLine.toString());

        assertTrue(!chunks.isEmpty());
        // Even a single long line should be chunked if it exceeds the size
        if (longLine.length() > 2048) {
            assertTrue(chunks.size() > 1);
        }
    }

    @Test
    void testChunkCode_MixedLineLengths() {
        StringBuilder code = new StringBuilder();
        // Add short lines
        for (int i = 0; i < 20; i++) {
            code.append("short\n");
        }
        // Add long lines
        for (int i = 0; i < 20; i++) {
            code.append("This is a much longer line with more content to test chunking behavior").append(i).append("\n");
        }

        List<String> chunks = chunkingService.chunkCode(code.toString());

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkCode_CodeWithMultipleNewlines() {
        String code = "line1\n\n\nline2\n\nline3\n\n\n\nline4";

        List<String> chunks = chunkingService.chunkCode(code);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // Verify newlines are preserved
        String concatenated = String.join("", chunks);
        assertTrue(concatenated.contains("\n\n\n"));
    }
}

