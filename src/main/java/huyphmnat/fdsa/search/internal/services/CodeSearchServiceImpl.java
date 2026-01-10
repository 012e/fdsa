package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.Indexes;
import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.dtos.CodeSearchResult;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeSearchServiceImpl implements CodeSearchService {
    private final OpenSearchClient openSearchClient;
    private static final String FILES_INDEX_NAME = Indexes.CODE_FILE_INDEX;

    @Override
    public CodeSearchResponse searchCode(CodeSearchRequest request) {
        log.info("Searching code with query: {}, page: {}, size: {}",
            request.getQuery(), request.getPage(), request.getSize());

        try {
            SearchRequest searchRequest = buildSearchRequest(request);
            SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);

            List<CodeSearchResult> results = response.hits().hits().stream()
                .map(this::mapHitToResult)
                .collect(Collectors.toList());

            long totalHits = response.hits().total().value();
            int totalPages = (int) Math.ceil((double) totalHits / request.getSize());

            return CodeSearchResponse.builder()
                .results(results)
                .totalHits(totalHits)
                .page(request.getPage())
                .size(request.getSize())
                .totalPages(totalPages)
                .tookMs(response.took())
                .build();

        } catch (Exception e) {
            log.error("Error searching code", e);
            throw new RuntimeException("Failed to search code", e);
        }
    }

    private SearchRequest buildSearchRequest(CodeSearchRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Add full-text search query
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(request.getQuery())
                .fields("content^3", "file_name^2", "file_path")  // Boost content and filename
            ).toQuery();
            boolQuery.must(multiMatchQuery);
        }

        // Add filters
        List<Query> filters = new ArrayList<>();

        if (request.getRepositoryId() != null) {
            filters.add(TermQuery.of(t -> t
                .field("repository_id")
                .value(FieldValue.of(request.getRepositoryId().toString()))
            ).toQuery());
        }

        if (request.getRepositoryIdentifier() != null && !request.getRepositoryIdentifier().isEmpty()) {
            filters.add(TermQuery.of(t -> t
                .field("repository_identifier.keyword")
                .value(FieldValue.of(request.getRepositoryIdentifier()))
            ).toQuery());
        }

        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            filters.add(TermQuery.of(t -> t
                .field("language.keyword")
                .value(FieldValue.of(request.getLanguage()))
            ).toQuery());
        }

        if (request.getFileExtension() != null && !request.getFileExtension().isEmpty()) {
            filters.add(TermQuery.of(t -> t
                .field("file_extension.keyword")
                .value(FieldValue.of(request.getFileExtension()))
            ).toQuery());
        }

        if (request.getFilePathPattern() != null && !request.getFilePathPattern().isEmpty()) {
            filters.add(WildcardQuery.of(w -> w
                .field("file_path")
                .value(request.getFilePathPattern())
            ).toQuery());
        }

        if (!filters.isEmpty()) {
            boolQuery.filter(filters);
        }

        // Build search request
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(FILES_INDEX_NAME)
            .query(boolQuery.build().toQuery())
            .from(request.getPage() * request.getSize())
            .size(request.getSize());

        // Add highlighting if requested
        if (request.getHighlightFields() != null && !request.getHighlightFields().isEmpty()) {
            Map<String, HighlightField> highlightFields = new HashMap<>();
            for (String field : request.getHighlightFields()) {
                highlightFields.put(field, HighlightField.of(h -> h
                    .numberOfFragments(3)
                    .fragmentSize(150)
                ));
            }

            searchBuilder.highlight(Highlight.of(h -> h
                .fields(highlightFields)
                .preTags("<mark>")
                .postTags("</mark>")
            ));
        }

        return searchBuilder.build();
    }

    private CodeSearchResult mapHitToResult(Hit<Map> hit) {
        Map<String, Object> source = hit.source();

        CodeSearchResult.CodeSearchResultBuilder builder = CodeSearchResult.builder()
            .id((String) source.get("id"))
            .repositoryId(UUID.fromString((String) source.get("repository_id")))
            .repositoryIdentifier((String) source.get("repository_identifier"))
            .filePath((String) source.get("file_path"))
            .fileName((String) source.get("file_name"))
            .fileExtension((String) source.get("file_extension"))
            .language((String) source.get("language"))
            .content((String) source.get("content"))
            .score(hit.score())
            .size(source.get("size") != null ? ((Number) source.get("size")).longValue() : null);

        // Parse timestamps
        if (source.get("created_at") != null) {
            builder.createdAt(Instant.parse((String) source.get("created_at")));
        }
        if (source.get("updated_at") != null) {
            builder.updatedAt(Instant.parse((String) source.get("updated_at")));
        }

        // Parse highlights
        if (hit.highlight() != null && !hit.highlight().isEmpty()) {
            Map<String, List<String>> highlights = new HashMap<>();
            hit.highlight().forEach((field, fragments) -> {
                highlights.put(field, fragments);
            });
            builder.highlights(highlights);
        }

        // Parse matched chunks if available
        if (source.get("chunks") != null) {
            List<Map<String, Object>> chunks = (List<Map<String, Object>>) source.get("chunks");
            List<CodeSearchResult.ChunkMatch> matchedChunks = chunks.stream()
                .map(chunk -> CodeSearchResult.ChunkMatch.builder()
                    .index(((Number) chunk.get("index")).intValue())
                    .content((String) chunk.get("content"))
                    .startLine(chunk.get("start_line") != null ? ((Number) chunk.get("start_line")).intValue() : 0)
                    .endLine(chunk.get("end_line") != null ? ((Number) chunk.get("end_line")).intValue() : 0)
                    .build())
                .collect(Collectors.toList());
            builder.matchedChunks(matchedChunks);
        }

        return builder.build();
    }
}

