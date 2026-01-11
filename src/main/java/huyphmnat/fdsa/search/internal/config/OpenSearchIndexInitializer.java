package huyphmnat.fdsa.search.internal.config;

import huyphmnat.fdsa.search.FieldNames;
import huyphmnat.fdsa.search.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.BuiltinScriptLanguage;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.cluster.PutClusterSettingsRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.ingest.Processor;
import org.opensearch.client.opensearch.ml.*;
import org.opensearch.client.opensearch.search_pipeline.ScoreCombinationTechnique;
import org.opensearch.client.opensearch.search_pipeline.ScoreNormalizationTechnique;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenSearchIndexInitializer implements ApplicationRunner {

    private final OpenSearchClient openSearchClient;
    private ConfigurableApplicationContext context;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    private static final String INGEST_PIPELINE_ID = "code-files-ingest-pipeline";
    private static final String SEARCH_PIPELINE_ID = "code-files-search-pipeline";
    private static final String FAKE_EMBEDDING_SCRIPT_ID = "fake_embedding_post_process";

    private void registerFakeEmbeddingPostProcessScript() throws IOException {

        openSearchClient.putScript(r -> r
                .id(FAKE_EMBEDDING_SCRIPT_ID)
                .script(s -> s
                        .lang(a -> a.builtin(BuiltinScriptLanguage.Painless))
                        .source(getScript())
                )
        );

        log.info("Stored script '{}' registered with hard-coded 1536-dim vector.", FAKE_EMBEDDING_SCRIPT_ID);
    }

    private static @NonNull String getScript() {
        // Generate the hard-coded array string: [0.0, 0.0, ..., 0.0]
        String hardCodedData = IntStream.range(0, 1536)
                .mapToObj(i -> "0.0")
                .collect(Collectors.joining(",", "[", "]"));
        return String.format("""
                def name = "sentence_embedding";
                def dataType = "FLOAT32";
                def data = %s;
                def shape = [1536];
                
                return "{"
                    + "\\"name\\":\\"" + name + "\\","
                    + "\\"data_type\\":\\"" + dataType + "\\","
                    + "\\"shape\\":" + shape + ","
                    + "\\"data\\":" + data
                    + "}";
                """, hardCodedData);
    }

    public static String MODEL_ID = null;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting OpenSearch Semantic Search Configuration...");
        configurateCluster();
        registerFakeEmbeddingPostProcessScript();

        var connector = setupOpenAIConnector();
        var group = registerModelGroup();
        var model = registerModel(connector, group);
        deployModel(model);
        MODEL_ID = model.modelId();

        setupIngestPipeline(model.modelId());

        createSearchPipeline();

        createCodeFilesIndexIfNotExists();

        log.info("OpenSearch initialization complete.");
    }

    private void configurateCluster() throws IOException {
        var request = PutClusterSettingsRequest.of(t ->
                t.persistent(Map.of("plugins.ml_commons.trusted_connector_endpoints_regex", JsonData.of(List.of(".*")),
                                "script.max_compilations_rate", JsonData.of("100000/1m")
                        )
                )

        );
        openSearchClient.cluster().putSettings(request);
    }

    private void createSearchPipeline() throws IOException {
        log.info("Configuring Search Pipeline '{}'...", SEARCH_PIPELINE_ID);

        openSearchClient.searchPipeline()
                .put(e -> e
                        .id(SEARCH_PIPELINE_ID)
                        .phaseResultsProcessors(t -> t
                                .normalizationProcessor(f -> f
                                        .description("Post processor for hybrid search")
                                        .normalization(g -> g.technique(ScoreNormalizationTechnique.MinMax))
                                        .combination(g -> g.technique(ScoreCombinationTechnique.ArithmeticMean)
                                                .parameters(z -> z.weights(0.3f, 0.7f))))
                        ));
        log.info("Search pipeline created.");
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
                        .fieldMap(Map.of(FieldNames.CONTENT, FieldNames.CONTENT_EMBEDDING)))
                .build();

        openSearchClient.ingest().putPipeline(p -> p
                .id(INGEST_PIPELINE_ID)
                .description("Pipeline for generating embeddings from code files")
                .processors(textEmbeddingProcessor)
        );
        log.info("Ingest pipeline '{}' configured.", INGEST_PIPELINE_ID);
    }

    private void createCodeFilesIndexIfNotExists() throws Exception {
        if (indexExists(Indexes.CODE_FILE_INDEX)) {
            log.info("Index {} already exists.", Indexes.CODE_FILE_INDEX);
            return;
        }

        CreateIndexRequest request = CreateIndexRequest.of(b -> b
                .index(Indexes.CODE_FILE_INDEX)
                .settings(IndexSettings.of(s -> s
                        .numberOfShards(1)
                        .numberOfReplicas(0)
                        .knn(true)
                        .defaultPipeline(INGEST_PIPELINE_ID)
                        .search(search -> search.defaultPipeline(SEARCH_PIPELINE_ID))
                ))
                .mappings(m -> m
                        .properties(FieldNames.ID, Property.of(p -> p.keyword(k -> k)))
                        .properties(FieldNames.REPOSITORY_ID, Property.of(p -> p.keyword(k -> k)))
                        .properties(FieldNames.REPOSITORY_IDENTIFIER, Property.of(p -> p.keyword(k -> k)))
                        .properties(FieldNames.FILE_PATH, Property.of(p -> p.text(t -> t
                                .fields("keyword", Property.of(f -> f.keyword(k -> k))))))
                        .properties(FieldNames.FILE_NAME, Property.of(p -> p.text(t -> t
                                .fields("keyword", Property.of(f -> f.keyword(k -> k))))))
                        .properties(FieldNames.FILE_EXTENSION, Property.of(p -> p.keyword(k -> k)))
                        .properties(FieldNames.LANGUAGE, Property.of(p -> p.keyword(k -> k)))
                        .properties(FieldNames.CONTENT, Property.of(p -> p.text(t -> t)))
                        .properties(FieldNames.CONTENT_EMBEDDING, Property.of(p -> p.knnVector(knn -> knn
                                .dimension(1536)
                                .method(method -> method
                                        .name("hnsw")
                                        .spaceType("cosinesimil")
                                        .engine("faiss")))))
                        .properties(FieldNames.SIZE, Property.of(p -> p.long_(l -> l)))
                        .properties(FieldNames.CHUNKS, Property.of(p -> p.nested(n -> n
                                .properties(FieldNames.CHUNK_INDEX, Property.of(cp -> cp.integer(i -> i)))
                                .properties(FieldNames.CHUNK_CONTENT, Property.of(cp -> cp.text(t -> t)))
                                .properties(FieldNames.CHUNK_EMBEDDING, Property.of(cp -> cp.knnVector(knn -> knn
                                        .dimension(1536)
                                        .method(method -> method
                                                .name("hnsw")
                                                .spaceType("cosinesimil")
                                                .engine("faiss")
                                                .parameters("ef_construction", JsonData.of(128))
                                                .parameters("m", JsonData.of(16))))))
                                .properties(FieldNames.CHUNK_START_LINE, Property.of(cp -> cp.integer(i -> i)))
                                .properties(FieldNames.CHUNK_END_LINE, Property.of(cp -> cp.integer(i -> i))))))
                        .properties(FieldNames.CREATED_AT, Property.of(p -> p.keyword(k -> k)))
                        .properties(FieldNames.UPDATED_AT, Property.of(p -> p.keyword(k -> k)))
                )
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