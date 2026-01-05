package huyphmnat.fdsa.shared.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenSearchIndexingService {

    private final OpenSearchClient openSearchClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX_NAME = "code_snippets";

    public void indexSnippet(
            String snippetId,
            String code,
            String overallSummary,
            List<Double> overallEmbedding,
            List<ChunkData> chunks) {

        log.info("Indexing snippet {} to OpenSearch", snippetId);

        Map<String, Object> document = new HashMap<>();
        document.put("snippet_id", snippetId);
        document.put("code", code);
        document.put("overall_summary", overallSummary);
        document.put("overall_embedding", overallEmbedding);
        document.put("created_at", Instant.now().toString());
        document.put("updated_at", Instant.now().toString());

        List<Map<String, Object>> chunksData = new ArrayList<>();
        for (ChunkData chunk : chunks) {
            Map<String, Object> chunkMap = new HashMap<>();
            chunkMap.put("chunk_index", chunk.index());
            chunkMap.put("code", chunk.code());
            chunkMap.put("summary", chunk.summary());
            chunkMap.put("embedding", chunk.embedding());
            chunksData.add(chunkMap);
        }
        document.put("chunks", chunksData);

        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .id(snippetId)
                .document(document)
            );

            IndexResponse response = openSearchClient.index(request);
            log.info("Indexed snippet {} with result: {}", snippetId, response.result());
        } catch (Exception e) {
            log.error("Failed to index snippet {} to OpenSearch", snippetId, e);
            throw new RuntimeException("Failed to index to OpenSearch", e);
        }
    }

    public record ChunkData(int index, String code, String summary, List<Double> embedding) {}
}

