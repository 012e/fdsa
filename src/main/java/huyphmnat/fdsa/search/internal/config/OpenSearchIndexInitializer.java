package huyphmnat.fdsa.search.internal.config;

import huyphmnat.fdsa.search.FieldNames;
import huyphmnat.fdsa.search.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.cluster.PutClusterSettingsRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.search_pipeline.ScoreRankerCombinationTechnique;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
// @Profile("opensearch-integration-testing")
public class OpenSearchIndexInitializer implements ApplicationRunner {

    private final OpenSearchClient openSearchClient;

    private static final String SEARCH_PIPELINE_ID = "code-files-search-pipeline";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting OpenSearch Semantic Search Configuration...");
        configurateCluster();

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
                        .phaseResultsProcessors(t -> t .scoreRankerProcessor(f -> f
                                        .combination(g -> g.technique(ScoreRankerCombinationTechnique.Rrf)))
                        ));
        log.info("Search pipeline created.");
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