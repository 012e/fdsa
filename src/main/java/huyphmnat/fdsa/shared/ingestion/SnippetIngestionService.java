package huyphmnat.fdsa.shared.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnippetIngestionService {

    private final OpenAIService openAIService;
    private final CodeChunkingService chunkingService;
    private final OpenSearchIndexingService indexingService;

    public void ingestSnippet(String snippetId, String code) {
        log.info("Starting ingestion for snippet {}", snippetId);

        try {
            // Step 1: Generate overall summary
            log.info("Generating overall summary...");
            String overallSummary = openAIService.summarizeCode(code);

            // Step 2: Generate embedding for overall summary
            log.info("Generating embedding for overall summary...");
            List<Double> overallEmbedding = openAIService.embedText(overallSummary);

            // Step 3: Chunk the code
            log.info("Chunking code...");
            List<String> chunks = chunkingService.chunkCode(code);
            log.info("Created {} chunks", chunks.size());

            // Step 4: Generate summaries for each chunk
            log.info("Generating chunk summaries...");
            List<String> chunkSummaries = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String summary = openAIService.summarizeChunk(chunks.get(i), i);
                chunkSummaries.add(summary);
            }

            // Step 5: Generate embeddings for all chunk summaries
            log.info("Generating embeddings for chunk summaries...");
            List<List<Double>> chunkEmbeddings = openAIService.embedTexts(chunkSummaries);

            // Step 6: Prepare chunk data
            List<OpenSearchIndexingService.ChunkData> chunkDataList = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                chunkDataList.add(new OpenSearchIndexingService.ChunkData(
                        i,
                        chunks.get(i),
                        chunkSummaries.get(i),
                        chunkEmbeddings.get(i)
                ));
            }

            // Step 7: Index to OpenSearch
            log.info("Indexing to OpenSearch...");
            indexingService.indexSnippet(
                    snippetId,
                    code,
                    overallSummary,
                    overallEmbedding,
                    chunkDataList
            );

            log.info("Successfully completed ingestion for snippet {}", snippetId);
        } catch (Exception e) {
            log.error("Failed to ingest snippet {}", snippetId, e);
            throw new RuntimeException("Ingestion failed", e);
        }
    }
}

