package huyphmnat.fdsa.search.internal.config;

import huyphmnat.fdsa.search.Indexes;
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


    @Override
    public void run(@SuppressWarnings("NullableProblems") ApplicationArguments args) {
        log.info("Initializing OpenSearch indices...");

        try {
            createCodeFilesIndexIfNotExists();
            log.info("OpenSearch indices initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize OpenSearch indices", e);
            // Don't throw - allow application to start even if OpenSearch is not ready
        }
    }

    private void createCodeFilesIndexIfNotExists() throws Exception {
        if (indexExists(Indexes.CODE_FILE_INDEX)) {
            log.info("Index '{}' already exists", Indexes.CODE_FILE_INDEX);
            return;
        }

        log.info("Creating index '{}'", Indexes.CODE_FILE_INDEX);

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
                "codeChunks": {
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
                "created_at": { "type": "string" },
                "updated_at": { "type": "string" }
              }
            }
            """;

        CreateIndexRequest request = CreateIndexRequest.of(b -> b
            .index(Indexes.CODE_FILE_INDEX)
            .settings(IndexSettings.of(s -> s
                .numberOfShards(1)
                .numberOfReplicas(0)
            ))
            .mappings(m -> m.withJson(new StringReader(mappings)))
        );

        openSearchClient.indices().create(request);
        log.info("Successfully created index '{}'", Indexes.CODE_FILE_INDEX);
    }

    private boolean indexExists(String indexName) throws Exception {
        ExistsRequest request = ExistsRequest.of(b -> b.index(indexName));
        BooleanResponse response = openSearchClient.indices().exists(request);
        return response.value();
    }
}

