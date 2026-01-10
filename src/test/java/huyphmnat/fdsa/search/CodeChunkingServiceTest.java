package huyphmnat.fdsa.search;

import huyphmnat.fdsa.search.internal.services.CodeChunkingService;
import huyphmnat.fdsa.search.internal.services.CodeChunkingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CodeChunkingServiceTest {

    private CodeChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new CodeChunkingServiceImpl();
    }

    @Test
    void testChunkSmallCode() {
        // Given: A small code snippet
        String code = "def hello():\n    print('world')";

        // When: Chunking the code
        List<String> chunks = chunkingService.chunkCode(code);

        // Then: Should return single chunk
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(code);
    }

    @Test
    void testChunkLargeCode() {
        // Given: A large code snippet
        StringBuilder largeCode = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            largeCode.append("def function_").append(i).append("():\n");
            largeCode.append("    return ").append(i).append("\n");
        }

        // When: Chunking the code
        List<String> chunks = chunkingService.chunkCode(largeCode.toString());

        // Then: Should return multiple chunks
        assertThat(chunks).hasSizeGreaterThan(1);

        // Verify each chunk has content
        for (String chunk : chunks) {
            assertThat(chunk).isNotEmpty();
        }
    }

    @Test
    void testChunkPreservesLineBreaks() {
        // Given: Code with specific line structure
        String code = "line1\nline2\nline3\nline4";

        // When: Chunking
        List<String> chunks = chunkingService.chunkCode(code);

        // Then: Should preserve newlines
        String combined = String.join("", chunks);
        assertThat(combined).contains("line1\n");
        assertThat(combined).contains("line2\n");
    }

    @Test
    void testEmptyCode() {
        // Given: Empty code
        String code = "";

        // When: Chunking
        List<String> chunks = chunkingService.chunkCode(code);

        // Then: Should return empty list or single empty chunk
        assertThat(chunks).isNotEmpty();
    }

    @Test
    void testChunkVeryLongSingleLine() {
        // Given: Very long single line
        String longLine = "x = " + "a".repeat(5000);

        // When: Chunking
        List<String> chunks = chunkingService.chunkCode(longLine);

        // Then: Should handle appropriately
        assertThat(chunks).isNotEmpty();
    }
}

