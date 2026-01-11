package huyphmnat.fdsa.search.internal.config;

import huyphmnat.fdsa.search.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.ingest.Processor;
import org.opensearch.client.opensearch.ml.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("opensearch-integration-testing")
public class OpenSearchIndexInitializer implements ApplicationRunner {

    private final OpenSearchClient openSearchClient;
    private ConfigurableApplicationContext context;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    private static final String PIPELINE_ID = "code-files-pipeline";

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting OpenSearch Semantic Search Configuration...");

            var connector = setupOpenAIConnector();
            var group = registerModelGroup();
            var model = registerModel(connector, group);
            deployModel(model);


            setupIngestPipeline(model.modelId());
            createCodeFilesIndexIfNotExists();

            log.info("OpenSearch initialization complete.");
        } catch (Exception e) {
            log.error("Failed to initialize OpenSearch", e);
            System.exit(SpringApplication.exit(context));
        }
    }

    private DeployModelResponse deployModel(RegisterModelResponse model) throws IOException {
        var deployModelRequest = DeployModelRequest.builder()
                .modelId(model.modelId())
                .build();
        return openSearchClient.ml().deployModel(deployModelRequest);
    }

    private RegisterModelResponse registerModel(CreateConnectorResponse connector, RegisterModelGroupResponse modelGroup) throws IOException {
        var registerModelRequest = RegisterModelRequest.builder()
                .name("OpenAI embedding model")
                .functionName("remote")
                .connectorId(connector.connectorId())
                .modelGroupId(modelGroup.modelGroupId())
                .description("test embedding model")
                .build();
        return openSearchClient.ml().registerModel(registerModelRequest);
    }

    private RegisterModelGroupResponse registerModelGroup() throws IOException {
        var registerModelGroupRequest = RegisterModelGroupRequest.builder()
                .name("OpenAI_embedding_model")
                .description("Test model group for OpenAI embedding model")
                .build();
        return openSearchClient.ml().registerModelGroup(registerModelGroupRequest);
    }

    private CreateConnectorResponse setupOpenAIConnector() throws Exception {
        log.info("Registering OpenAI Connector...");

        CreateConnectorRequest request = CreateConnectorRequest.of(c -> c
                .name("openai-connector")
                .description("Connector for OpenAI Embeddings")
                .protocol("http")
                .parameters(Map.of("model", JsonData.of("text-embedding-3-small")))
                .credential(t -> t.metadata(Map.of("api_key", JsonData.of(openAiApiKey))))
                .version(1)
                .actions(a -> a
                        .actionType("predict")
                        .method("POST")
                        .url("https://api.openai.com/v1/embeddings")
                        .headers(t -> t.metadata(Map.of("Authorization", JsonData.of("Bearer ${credential.api_key}"))))
                        .requestBody("{ \"input\": ${parameters.input}, \"model\": \"${parameters.model}\" }")
                        .preProcessFunction("connector.pre_process.openai.embedding")
                        .postProcessFunction("connector.post_process.openai.embedding")
                )
        );


        return openSearchClient.ml().createConnector(request);
    }

    private void setupIngestPipeline(String modelId) throws Exception {
        var textEmbeddingProcessor = new Processor.Builder()
                .textEmbedding(t -> t
                        .modelId(modelId)
                        .fieldMap(Map.of("content", "content_embedding")))
                .build();

        openSearchClient.ingest().putPipeline(p -> p
                .id(PIPELINE_ID)
                .description("Pipeline for generating embeddings from code files")
                .processors(textEmbeddingProcessor)
        );
        log.info("Ingest pipeline '{}' configured.", PIPELINE_ID);
    }

    private void createCodeFilesIndexIfNotExists() throws Exception {
        if (indexExists(Indexes.CODE_FILE_INDEX)) {
            log.info("Index {} already exists.", Indexes.CODE_FILE_INDEX);
            return;
        }

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
                    "content_embedding": {
                      "type": "knn_vector",
                      "dimension": 1536,
                      "method": {
                        "name": "hnsw",
                        "space_type": "cosinesimil",
                        "engine": "nmslib"
                      }
                    },
                    "size": { "type": "long" },
                    "chunks": {
                      "type": "nested",
                      "properties": {
                        "index": { "type": "integer" },
                        "content": {
                          "type": "text"
                        },
                        "embedding": {
                          "type": "knn_vector",
                          "dimension": 1536,
                          "method": {
                            "name": "hnsw",
                            "space_type": "cosinesimil",
                            "engine": "faiss",
                            "parameters": {
                              "ef_construction": 128,
                              "m": 16
                            }
                          }
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
                        .knn(true)
//                        .defaultPipeline(PIPELINE_ID)
                ))
                .mappings(m -> m.withJson(new StringReader(mappings)))
        );

        openSearchClient.indices().create(request);
        log.info("Index {} created successfully.", Indexes.CODE_FILE_INDEX);
    }

    private boolean indexExists(String indexName) throws Exception {
        return openSearchClient
                .indices()
                .exists(e -> e.index(indexName))
                .value();
    }
}