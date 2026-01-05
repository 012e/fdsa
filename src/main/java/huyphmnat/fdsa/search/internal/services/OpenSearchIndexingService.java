package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.internal.models.CodeFileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
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

    private static final String SNIPPETS_INDEX_NAME = "code_snippets";
    private static final String FILES_INDEX_NAME = "code_files";

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
                .index(SNIPPETS_INDEX_NAME)
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

    /**
     * Index a single code file to OpenSearch
     */
    public void indexCodeFile(CodeFileDocument document) {
        log.info("Indexing code file {} from repository {}",
            document.getFilePath(), document.getRepositoryIdentifier());

        Map<String, Object> docMap = buildCodeFileDocumentMap(document);

        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(FILES_INDEX_NAME)
                .id(document.getId())
                .document(docMap)
            );

            IndexResponse response = openSearchClient.index(request);
            log.info("Indexed code file {} with result: {}", document.getFilePath(), response.result());
        } catch (Exception e) {
            log.error("Failed to index code file {} to OpenSearch", document.getFilePath(), e);
            throw new RuntimeException("Failed to index code file to OpenSearch", e);
        }
    }

    /**
     * Bulk index multiple code files to OpenSearch
     */
    public void bulkIndexCodeFiles(List<CodeFileDocument> documents) {
        if (documents.isEmpty()) {
            log.info("No documents to index");
            return;
        }

        log.info("Bulk indexing {} code files to OpenSearch", documents.size());

        List<BulkOperation> operations = new ArrayList<>();
        for (CodeFileDocument document : documents) {
            Map<String, Object> docMap = buildCodeFileDocumentMap(document);

            operations.add(BulkOperation.of(b -> b
                .index(IndexOperation.of(i -> i
                    .index(FILES_INDEX_NAME)
                    .id(document.getId())
                    .document(docMap)
                ))
            ));
        }

        try {
            BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(operations)
            );

            BulkResponse response = openSearchClient.bulk(bulkRequest);

            if (response.errors()) {
                log.warn("Bulk indexing completed with errors. Failed items: {}",
                    response.items().stream()
                        .filter(item -> item.error() != null)
                        .count());
            } else {
                log.info("Successfully bulk indexed {} code files", documents.size());
            }
        } catch (Exception e) {
            log.error("Failed to bulk index code files to OpenSearch", e);
            throw new RuntimeException("Failed to bulk index code files to OpenSearch", e);
        }
    }

    private Map<String, Object> buildCodeFileDocumentMap(CodeFileDocument document) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", document.getId());
        docMap.put("repository_id", document.getRepositoryId().toString());
        docMap.put("repository_identifier", document.getRepositoryIdentifier());
        docMap.put("file_path", document.getFilePath());
        docMap.put("file_name", document.getFileName());
        docMap.put("file_extension", document.getFileExtension());
        docMap.put("language", document.getLanguage());
        docMap.put("content", document.getContent());
        docMap.put("size", document.getSize());
        docMap.put("created_at", document.getCreatedAt().toString());
        docMap.put("updated_at", document.getUpdatedAt().toString());

        if (document.getChunks() != null && !document.getChunks().isEmpty()) {
            List<Map<String, Object>> chunksData = new ArrayList<>();
            for (CodeFileDocument.CodeChunk chunk : document.getChunks()) {
                Map<String, Object> chunkMap = new HashMap<>();
                chunkMap.put("index", chunk.getIndex());
                chunkMap.put("content", chunk.getContent());
                chunkMap.put("start_line", chunk.getStartLine());
                chunkMap.put("end_line", chunk.getEndLine());
                chunksData.add(chunkMap);
            }
            docMap.put("chunks", chunksData);
        }

        return docMap;
    }

    public record ChunkData(int index, String code, String summary, List<Double> embedding) {}
}

