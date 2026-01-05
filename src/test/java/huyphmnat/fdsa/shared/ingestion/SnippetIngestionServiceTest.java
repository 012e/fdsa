package huyphmnat.fdsa.shared.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnippetIngestionServiceTest {

    @Mock
    private OpenAIService openAIService;

    @Mock
    private CodeChunkingService chunkingService;

    @Mock
    private OpenSearchIndexingService indexingService;

    @Captor
    private ArgumentCaptor<String> snippetIdCaptor;

    @Captor
    private ArgumentCaptor<String> codeCaptor;

    @Captor
    private ArgumentCaptor<String> summaryCaptor;

    @Captor
    private ArgumentCaptor<List<Double>> embeddingCaptor;

    @Captor
    private ArgumentCaptor<List<OpenSearchIndexingService.ChunkData>> chunksCaptor;

    private SnippetIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new SnippetIngestionService(
                openAIService,
                chunkingService,
                indexingService
        );
    }

    @Test
    void testIngestSnippetSuccess() {
        // Given: Mock responses
        String snippetId = "test-snippet-123";
        String code = "def hello(): print('world')";
        String overallSummary = "A simple hello world function";
        List<Double> overallEmbedding = createMockEmbedding();

        List<String> chunks = Arrays.asList(code);
        List<String> chunkSummaries = Arrays.asList("Hello world function");
        List<List<Double>> chunkEmbeddings = Arrays.asList(createMockEmbedding());

        when(openAIService.summarizeCode(code)).thenReturn(overallSummary);
        when(openAIService.embedText(overallSummary)).thenReturn(overallEmbedding);
        when(chunkingService.chunkCode(code)).thenReturn(chunks);
        when(openAIService.summarizeChunk(anyString(), anyInt())).thenReturn(chunkSummaries.get(0));
        when(openAIService.embedTexts(anyList())).thenReturn(chunkEmbeddings);

        // When: Ingest snippet
        ingestionService.ingestSnippet(snippetId, code);

        // Then: Verify all steps executed
        verify(openAIService).summarizeCode(code);
        verify(openAIService).embedText(overallSummary);
        verify(chunkingService).chunkCode(code);
        verify(openAIService).summarizeChunk(code, 0);
        verify(openAIService).embedTexts(chunkSummaries);

        verify(indexingService).indexSnippet(
                snippetIdCaptor.capture(),
                codeCaptor.capture(),
                summaryCaptor.capture(),
                embeddingCaptor.capture(),
                chunksCaptor.capture()
        );

        assertThat(snippetIdCaptor.getValue()).isEqualTo(snippetId);
        assertThat(codeCaptor.getValue()).isEqualTo(code);
        assertThat(summaryCaptor.getValue()).isEqualTo(overallSummary);
        assertThat(embeddingCaptor.getValue()).isEqualTo(overallEmbedding);
        assertThat(chunksCaptor.getValue()).hasSize(1);
    }

    @Test
    void testIngestSnippetWithMultipleChunks() {
        // Given: Code that produces multiple chunks
        String snippetId = "test-snippet-456";
        String code = "long code here";
        String overallSummary = "Overall summary";
        List<Double> overallEmbedding = createMockEmbedding();

        List<String> chunks = Arrays.asList("chunk1", "chunk2", "chunk3");
        List<String> chunkSummaries = Arrays.asList("Summary 1", "Summary 2", "Summary 3");
        List<List<Double>> chunkEmbeddings = Arrays.asList(
                createMockEmbedding(),
                createMockEmbedding(),
                createMockEmbedding()
        );

        when(openAIService.summarizeCode(code)).thenReturn(overallSummary);
        when(openAIService.embedText(overallSummary)).thenReturn(overallEmbedding);
        when(chunkingService.chunkCode(code)).thenReturn(chunks);
        when(openAIService.summarizeChunk("chunk1", 0)).thenReturn("Summary 1");
        when(openAIService.summarizeChunk("chunk2", 1)).thenReturn("Summary 2");
        when(openAIService.summarizeChunk("chunk3", 2)).thenReturn("Summary 3");
        when(openAIService.embedTexts(chunkSummaries)).thenReturn(chunkEmbeddings);

        // When: Ingest snippet
        ingestionService.ingestSnippet(snippetId, code);

        // Then: Verify all chunks processed
        verify(openAIService, times(3)).summarizeChunk(anyString(), anyInt());
        verify(indexingService).indexSnippet(
                eq(snippetId),
                eq(code),
                eq(overallSummary),
                eq(overallEmbedding),
                chunksCaptor.capture()
        );

        List<OpenSearchIndexingService.ChunkData> capturedChunks = chunksCaptor.getValue();
        assertThat(capturedChunks).hasSize(3);
        assertThat(capturedChunks.get(0).index()).isEqualTo(0);
        assertThat(capturedChunks.get(1).index()).isEqualTo(1);
        assertThat(capturedChunks.get(2).index()).isEqualTo(2);
    }

    @Test
    void testIngestSnippetHandlesException() {
        // Given: Service that throws exception
        String snippetId = "error-snippet";
        String code = "error code";

        when(openAIService.summarizeCode(code))
                .thenThrow(new RuntimeException("OpenAI API error"));

        // When/Then: Should throw runtime exception
        try {
            ingestionService.ingestSnippet(snippetId, code);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Ingestion failed");
        }

        // Verify no indexing occurred
        verify(indexingService, never()).indexSnippet(
                anyString(), anyString(), anyString(), anyList(), anyList()
        );
    }

    private List<Double> createMockEmbedding() {
        Double[] embedding = new Double[1024];
        Arrays.fill(embedding, 0.1);
        return Arrays.asList(embedding);
    }
}

