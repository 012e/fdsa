package huyphmnat.fdsa.search.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response object containing code search results with pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSearchResponse {

    /**
     * List of search results
     */
    private List<CodeSearchResult> results;

    /**
     * Total number of hits
     */
    private long totalHits;

    /**
     * Current page number (0-based)
     */
    private int page;

    /**
     * Number of results per page
     */
    private int size;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Time taken to execute the search (in milliseconds)
     */
    private long tookMs;
}

