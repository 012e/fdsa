package huyphmnat.fdsa.search.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request object for searching code files in OpenSearch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSearchRequest {

    /**
     * Search query text (full-text search across file content, name, and path)
     */
    private String query;

    /**
     * Optional pre-computed embedding vector for semantic search
     * If not provided but hybridSearch is enabled, will be generated from query
     */
    private float[] queryEmbedding;

    /**
     * Filter by repository identifier (owner/name) (optional)
     */
    private String repositoryIdentifier;

    /**
     * Filter by programming language (optional)
     */
    private String language;

    /**
     * Filter by file path pattern (optional)
     */
    private String filePathPattern;

    /**
     * Page number (0-based)
     */
    @Builder.Default
    private int page = 0;

    /**
     * Number of results per page
     */
    @Builder.Default
    private int size = 10;

    /**
     * Fields to highlight in search results (optional)
     */
    private List<String> highlightFields;
}


