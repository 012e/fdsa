package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.FieldNames;
import huyphmnat.fdsa.search.Indexes;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteByQueryResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        try {
            IndexRequest<CodeFileDocument> request = IndexRequest.of(i -> i
                    .index(Indexes.CODE_FILE_INDEX)
                    .id(document.getId().toString())
                    .document(document)
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
            operations.add(BulkOperation.of(b -> b
                    .index(IndexOperation.of(i -> i
                            .index(Indexes.CODE_FILE_INDEX)
                            .id(document.getId().toString())
                            .document(document)
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

    @Override
    public void refreshIndexes() {
        try {
            openSearchClient.indices().refresh(r -> r.index(Indexes.CODE_FILE_INDEX));
            log.info("Refreshed index '{}'", Indexes.CODE_FILE_INDEX);
        } catch (Exception e) {
            log.error("Failed to refresh index '{}'", Indexes.CODE_FILE_INDEX, e);
            throw new RuntimeException("Failed to refresh index", e);
        }
    }

    @Override
    public void deleteFilesByPaths(UUID repositoryId, List<String> filePaths) {
        if (filePaths.isEmpty()) {
            log.info("No files to delete");
            return;
        }

        log.info("Deleting {} files from repository {} in OpenSearch", filePaths.size(), repositoryId);

        try {
            // Build query: repository_id = repositoryId AND file_path IN (filePaths)
            Query repositoryQuery = TermQuery.of(t -> t
                .field(FieldNames.REPOSITORY_ID)
                .value(FieldValue.of(repositoryId.toString()))
            ).toQuery();

            Query filePathsQuery = TermsQuery.of(t -> t
                .field(FieldNames.FILE_PATH_KEYWORD)
                .terms(TermsQueryField.of(f -> f.value(
                    filePaths.stream().map(FieldValue::of).toList()
                )))
            ).toQuery();

            Query boolQuery = BoolQuery.of(b -> b
                .must(repositoryQuery)
                .must(filePathsQuery)
            ).toQuery();

            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                .index(Indexes.CODE_FILE_INDEX)
                .query(boolQuery)
            );

            DeleteByQueryResponse response = openSearchClient.deleteByQuery(request);

            log.info("Deleted {} documents from index for repository {}",
                response.deleted(), repositoryId);
        } catch (Exception e) {
            log.error("Failed to delete files from OpenSearch for repository {}", repositoryId, e);
            throw new RuntimeException("Failed to delete files from OpenSearch", e);
        }
    }
}
