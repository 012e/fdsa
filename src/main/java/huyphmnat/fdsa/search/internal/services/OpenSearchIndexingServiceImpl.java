package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.Indexes;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenSearchIndexingServiceImpl implements OpenSearchIndexingService {
    private final OpenSearchClient openSearchClient;

    /**
     * Index a single code file to OpenSearch
     */
    @Override
    public void indexCodeFile(CodeFileDocument document) {
        log.info("Indexing code file {} from repository {}",
                document.getFilePath(), document.getRepositoryIdentifier());

        Map<String, Object> docMap = buildCodeFileDocumentMap(document);

        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(Indexes.CODE_FILE_INDEX)
                    .id(document.getId().toString())
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
    @Override
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
                            .index(Indexes.CODE_FILE_INDEX)
                            .id(document.getId().toString())
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

}

