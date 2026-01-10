package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.FieldNames;
import huyphmnat.fdsa.search.Indexes;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.dtos.CodeSearchResult;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import huyphmnat.fdsa.search.internal.config.OpenSearchIndexInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
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
            SearchResponse<CodeFileDocument> response = openSearchClient.search(searchRequest, CodeFileDocument.class);

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
        return buildHybridSearchRequest(request);
    }

    /**
     * Builds a hybrid search request using RRF (Reciprocal Rank Fusion)
     * Combines full-text search with vector search
     */
    private SearchRequest buildHybridSearchRequest(CodeSearchRequest request) {
        try {
            // Build hybrid query with RRF
            Query hybridQuery = buildHybridQuery(request);

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(FILES_INDEX_NAME)
                    .query(hybridQuery)
                    .from(request.getPage() * request.getSize())
                    .pipeline()
                    .size(request.getSize());

            // Add highlighting if requested
            addHighlighting(searchBuilder, request);

            return searchBuilder.build();

        } catch (Exception e) {
            log.error("Failed to build hybrid search request, falling back to keyword search", e);
            return buildKeywordSearchRequest(request);
        }
    }

    /**
     * Builds a traditional keyword-based search request
     */
    private SearchRequest buildKeywordSearchRequest(CodeSearchRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Add full-text search query
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(request.getQuery())
                    .fields(FieldNames.CONTENT + "^3", FieldNames.FILE_NAME + "^2", FieldNames.FILE_PATH)  // Boost content and filename
            ).toQuery();
            boolQuery.must(multiMatchQuery);
        }

        // Add filters
        addFilters(boolQuery, request);

        // Build search request
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(FILES_INDEX_NAME)
                .query(boolQuery.build().toQuery())
                .from(request.getPage() * request.getSize())
                .size(request.getSize());

        // Add highlighting if requested
        addHighlighting(searchBuilder, request);

        return searchBuilder.build();
    }

    /**
     * Adds filter queries to the bool query builder
     */
    private void addFilters(BoolQuery.Builder boolQuery, CodeSearchRequest request) {
        List<Query> filters = new ArrayList<>();

        if (request.getRepositoryIdentifier() != null && !request.getRepositoryIdentifier().isEmpty()) {
            filters.add(TermQuery.of(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER_KEYWORD)
                    .value(FieldValue.of(request.getRepositoryIdentifier()))
            ).toQuery());
        }

        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            filters.add(TermQuery.of(t -> t
                    .field(FieldNames.LANGUAGE_KEYWORD)
                    .value(FieldValue.of(request.getLanguage()))
            ).toQuery());
        }

        if (request.getFilePathPattern() != null && !request.getFilePathPattern().isEmpty()) {
            filters.add(WildcardQuery.of(w -> w
                    .field(FieldNames.FILE_PATH_KEYWORD)
                    .value(request.getFilePathPattern())
            ).toQuery());
        }

        if (!filters.isEmpty()) {
            boolQuery.filter(filters);
        }
    }

    /**
     * Adds highlighting configuration to the search request builder
     */
    private void addHighlighting(SearchRequest.Builder searchBuilder, CodeSearchRequest request) {
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
    }

    /**
     * Builds a hybrid query that combines keyword and vector search with RRF
     * This uses OpenSearch's native HybridQuery API
     */
    private Query buildHybridQuery(CodeSearchRequest request) {
        List<Query> queries = new ArrayList<>();

        // 1. Keyword search query (multi_match)
        Query keywordQuery = MultiMatchQuery.of(m -> m
                .query(request.getQuery())
                .fields(FieldNames.CONTENT + "^3", FieldNames.FILE_NAME + "^2", FieldNames.FILE_PATH)
        ).toQuery();
        queries.add(keywordQuery);

        // 2. Vector search query (kNN)
        Query vectorQuery = NeuralQuery.of(k -> k
                .field(FieldNames.CONTENT_EMBEDDING)
                .modelId(OpenSearchIndexInitializer.MODEL_ID)
                .k(request.getSize() * 2)
        ).toQuery();
        queries.add(vectorQuery);

        // Build hybrid query with both queries
        Query hybridQuery = HybridQuery.of(h -> h
                .queries(queries)
        ).toQuery();

        // Wrap with filters if present
        if (hasFilters(request)) {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .must(hybridQuery);
            addFilters(boolQuery, request);
            return boolQuery.build().toQuery();
        }

        return hybridQuery;
    }

    private boolean hasFilters(CodeSearchRequest request) {
        return (request.getRepositoryIdentifier() != null && !request.getRepositoryIdentifier().isEmpty()) ||
                (request.getLanguage() != null && !request.getLanguage().isEmpty()) ||
                (request.getFilePathPattern() != null && !request.getFilePathPattern().isEmpty());
    }


    private CodeSearchResult mapHitToResult(Hit<CodeFileDocument> hit) {
        CodeFileDocument doc = hit.source();

        CodeSearchResult.CodeSearchResultBuilder builder = CodeSearchResult.builder()
                .id(doc.getId())
                .repositoryId(doc.getRepositoryId())
                .repositoryIdentifier(doc.getRepositoryIdentifier())
                .filePath(doc.getFilePath())
                .fileName(doc.getFileName())
                .fileExtension(doc.getFileExtension())
                .language(doc.getLanguage())
                .content(doc.getContent())
                .score(hit.score())
                .size(doc.getSize());

        // Parse timestamps
        if (doc.getCreatedAt() != null) {
            builder.createdAt(doc.getCreatedAt());
        }
        if (doc.getUpdatedAt() != null) {
            builder.updatedAt(doc.getUpdatedAt());
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
        if (doc.getCodeChunks() != null) {
            List<CodeSearchResult.ChunkMatch> matchedChunks = doc.getCodeChunks().stream()
                    .map(chunk -> CodeSearchResult.ChunkMatch.builder()
                            .index(chunk.getIndex())
                            .content(chunk.getContent())
                            .startLine(chunk.getStartLine() != null ? chunk.getStartLine() : 0)
                            .endLine(chunk.getEndLine() != null ? chunk.getEndLine() : 0)
                            .build())
                    .collect(Collectors.toList());
            builder.matchedChunks(matchedChunks);
        }

        return builder.build();
    }
}

