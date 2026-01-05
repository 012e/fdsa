package huyphmnat.fdsa.search.internal.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.StringReader;

/**
 * Initializes OpenSearch indices on application startup.
 * Creates indices with proper mappings if they don't exist.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpenSearchIndexInitializer implements ApplicationRunner {

    private final OpenSearchClient openSearchClient;

    private static final String CODE_FILES_INDEX = "code_files";
    private static final String CODE_SNIPPETS_INDEX = "code_snippets";

    @Override
    public void run(@SuppressWarnings("NullableProblems") ApplicationArguments args) {
        log.info("Initializing OpenSearch indices...");

        try {
            createCodeFilesIndexIfNotExists();
            createCodeSnippetsIndexIfNotExists();
            log.info("OpenSearch indices initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize OpenSearch indices", e);
            // Don't throw - allow application to start even if OpenSearch is not ready
        }
    }

    private void createCodeFilesIndexIfNotExists() throws Exception {
        if (indexExists(CODE_FILES_INDEX)) {
            log.info("Index '{}' already exists", CODE_FILES_INDEX);
            return;
        }

        log.info("Creating index '{}'", CODE_FILES_INDEX);

        String mappings = """
            {
              "properties": {
                "id": { "type": "keyword" },
                "repository_id": { "type": "keyword" },
                "repository_identifier": { "type": "keyword" },
                "file_path": {
                  "type": "text",
                  "fields": {
                    "keyword": { "type": "keyword" }
                  }
                },
                "file_name": {
                  "type": "text",
                  "fields": {
                    "keyword": { "type": "keyword" }
                  }
                },
                "file_extension": { "type": "keyword" },
                "language": { "type": "keyword" },
                "content": {
                  "type": "text"
                },
                "size": { "type": "long" },
                "chunks": {
                  "type": "nested",
                  "properties": {
                    "index": { "type": "integer" },
                    "content": {
                      "type": "text"
                    },
                    "start_line": { "type": "integer" },
                    "end_line": { "type": "integer" }
                  }
                },
                "created_at": { "type": "date" },
                "updated_at": { "type": "date" }
              }
            }
            """;

        CreateIndexRequest request = CreateIndexRequest.of(b -> b
            .index(CODE_FILES_INDEX)
            .settings(IndexSettings.of(s -> s
                .numberOfShards(1)
                .numberOfReplicas(0)
            ))
            .mappings(m -> m.withJson(new StringReader(mappings)))
        );

        openSearchClient.indices().create(request);
        log.info("Successfully created index '{}'", CODE_FILES_INDEX);
    }

    private void createCodeSnippetsIndexIfNotExists() throws Exception {
        if (indexExists(CODE_SNIPPETS_INDEX)) {
            log.info("Index '{}' already exists", CODE_SNIPPETS_INDEX);
            return;
        }

        log.info("Creating index '{}'", CODE_SNIPPETS_INDEX);

        String mappings = """
            {
              "properties": {
                "snippet_id": { "type": "keyword" },
                "code": { "type": "text" },
                "overall_summary": { "type": "text" },
                "overall_embedding": {
                  "type": "dense_vector",
                  "dims": 1536
                },
                "chunks": {
                  "type": "nested",
                  "properties": {
                    "chunk_index": { "type": "integer" },
                    "code": { "type": "text" },
                    "summary": { "type": "text" },
                    "embedding": {
                      "type": "dense_vector",
                      "dims": 1536
                    }
                  }
                },
                "created_at": { "type": "date" },
                "updated_at": { "type": "date" }
              }
            }
            """;

        CreateIndexRequest request = CreateIndexRequest.of(b -> b
            .index(CODE_SNIPPETS_INDEX)
            .settings(IndexSettings.of(s -> s
                .numberOfShards(1)
                .numberOfReplicas(0)
            ))
            .mappings(m -> m.withJson(new StringReader(mappings)))
        );

        openSearchClient.indices().create(request);
        log.info("Successfully created index '{}'", CODE_SNIPPETS_INDEX);
    }

    private boolean indexExists(String indexName) throws Exception {
        ExistsRequest request = ExistsRequest.of(b -> b.index(indexName));
        BooleanResponse response = openSearchClient.indices().exists(request);
        return response.value();
    }
}

