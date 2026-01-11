package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.MockEmbeddingModel;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.internal.services.CodeChunkingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CodeChunkingService with embeddings enabled
 */
class CodeChunkingServiceWithEmbeddingsTest {

    private CodeChunkingServiceImpl chunkingService;
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = new MockEmbeddingModel();
        chunkingService = new CodeChunkingServiceImpl(embeddingModel);
        // Enable embeddings for these tests
        ReflectionTestUtils.setField(chunkingService, "embeddingsEnabled", true);
    }

    @Test
    void testChunkCodeWithMetadata_SmallCode_ShouldReturnSingleChunkWithEmbedding() {
        String code = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}";

        List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(code);

        assertThat(chunks).hasSize(1);

        CodeFileDocument.CodeChunk chunk = chunks.get(0);
        assertThat(chunk.getIndex()).isEqualTo(0);
        assertThat(chunk.getContent()).isEqualTo(code);
        assertThat(chunk.getStartLine()).isEqualTo(1);
        assertThat(chunk.getEndLine()).isEqualTo(5);

        // Verify embedding is generated
        assertThat(chunk.getEmbedding()).isNotNull();
        assertThat(chunk.getEmbedding()).hasSize(1536); // Default dimension
        assertThat(chunk.getEmbedding()).isNotEmpty();
    }

    @Test
    void testChunkCodeWithMetadata_LargeCode_ShouldReturnMultipleChunksWithEmbeddings() {
        // Create code larger than chunk size (512 tokens * 4 chars = 2048 chars)
        StringBuilder largeCode = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeCode.append("public class Class").append(i).append(" {\n");
            largeCode.append("    private String field").append(i).append(";\n");
            largeCode.append("    public void method").append(i).append("() { }\n");
            largeCode.append("}\n\n");
        }

        List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(largeCode.toString());

        assertThat(chunks.size()).isGreaterThan(1);

        // Verify each chunk has proper metadata
        for (int i = 0; i < chunks.size(); i++) {
            CodeFileDocument.CodeChunk chunk = chunks.get(i);

            assertThat(chunk.getIndex()).isEqualTo(i);
            assertThat(chunk.getContent()).isNotEmpty();
            assertThat(chunk.getStartLine()).isGreaterThan(0);
            assertThat(chunk.getEndLine()).isGreaterThanOrEqualTo(chunk.getStartLine());

            // Verify embedding
            assertThat(chunk.getEmbedding()).isNotNull();
            assertThat(chunk.getEmbedding()).hasSize(1536);
        }

        // Verify line numbers are sequential
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getStartLine())
                .isEqualTo(chunks.get(i - 1).getEndLine() + 1);
        }
    }

    @Test
    void testChunkCodeWithMetadata_WithEmbeddingsDisabled_ShouldReturnEmptyEmbeddings() {
        // Disable embeddings
        ReflectionTestUtils.setField(chunkingService, "embeddingsEnabled", false);

        String code = "public class Test {\n    // test\n}";

        List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(code);

        assertThat(chunks).hasSize(1);

        CodeFileDocument.CodeChunk chunk = chunks.get(0);
        assertThat(chunk.getEmbedding()).isEmpty();
    }

    @Test
    void testChunkCodeWithMetadata_LineNumbersAreCorrect() {
        // Create code with known line structure
        String code = "line1\nline2\nline3\nline4\nline5\nline6\nline7\nline8\nline9\nline10";

        List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(code);

        assertThat(chunks).isNotEmpty();

        // First chunk should start at line 1
        assertThat(chunks.get(0).getStartLine()).isEqualTo(1);

        // Last chunk should end at the total number of lines
        int expectedTotalLines = code.split("\n").length;
        assertThat(chunks.get(chunks.size() - 1).getEndLine()).isEqualTo(expectedTotalLines);
    }

    @Test
    void testChunkCodeWithMetadata_PreservesContent() {
        String code = "public class Test {\n    private int x;\n    public int getX() { return x; }\n}";

        List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(code);

        // Reconstruct original code from chunks
        StringBuilder reconstructed = new StringBuilder();
        for (CodeFileDocument.CodeChunk chunk : chunks) {
            reconstructed.append(chunk.getContent());
        }

        assertThat(reconstructed.toString()).isEqualTo(code);
    }

    @Test
    void testChunkCodeWithMetadata_EmbeddingsAreDeterministic() {
        String code = "public class Deterministic { }";

        // Generate chunks twice
        List<CodeFileDocument.CodeChunk> chunks1 = chunkingService.chunkCodeWithMetadata(code);
        List<CodeFileDocument.CodeChunk> chunks2 = chunkingService.chunkCodeWithMetadata(code);

        assertThat(chunks1).hasSize(chunks2.size());

        // Embeddings should be identical for the same content
        for (int i = 0; i < chunks1.size(); i++) {
            List<Float> embedding1 = chunks1.get(i).getEmbedding();
            List<Float> embedding2 = chunks2.get(i).getEmbedding();

            assertThat(embedding1).hasSize(embedding2.size());

            // Compare embeddings (should be identical with mock)
            for (int j = 0; j < embedding1.size(); j++) {
                assertThat(embedding1.get(j)).isCloseTo(embedding2.get(j), org.assertj.core.data.Offset.offset(0.0001f));
            }
        }
    }
}

